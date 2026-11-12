package com.parca.yedekparca.config;

import com.parca.yedekparca.controller.DemoProductPhotoCatalog;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Standalone, opt-in updater for existing demo SVG OfferPhoto rows. Does not start Spring or the seeder. */
public final class DemoOfferPhotoUpdater {
    private static final String EXPECTED_DATABASE = "yedekparca_demo_db";
    private static final String DEMO_OEM = "DEMO-REF-%";
    private static final String DEMO_URLS = "('/api/dev-demo/images/brake.svg','/api/dev-demo/images/filter.svg','/api/dev-demo/images/engine.svg')";
    private static final String DEMO_PHOTO_SCOPE = " from offer_photos p join product_offers o on o.id=p.offer_id"
            + " join catalog_products c on c.id=o.catalog_product_id where c.oem_number like '" + DEMO_OEM + "'";
    private static final String CANDIDATE_QUERY = "select p.id,p.offer_id,p.object_key,p.public_url,p.display_order,p.primary_photo,c.name"
            + DEMO_PHOTO_SCOPE + " and p.object_key like 'demo-local/%' and p.public_url in " + DEMO_URLS
            + " order by p.id";
    private static final Map<String, String> COUNT_QUERIES = Map.of(
            "users", "select count(*) from users",
            "catalog products", "select count(*) from catalog_products",
            "offers", "select count(*) from product_offers",
            "offer photos", "select count(*) from offer_photos",
            "demo users", "select count(*) from users where email like 'demo-ph-%@sandbox.invalid'",
            "demo catalog products", "select count(*) from catalog_products where oem_number like 'DEMO-REF-%'",
            "demo offers", "select count(*) from product_offers o join catalog_products c on c.id=o.catalog_product_id where c.oem_number like 'DEMO-REF-%'",
            "demo offer photos", "select count(*)" + DEMO_PHOTO_SCOPE,
            "demo SVG photos", "select count(*)" + DEMO_PHOTO_SCOPE + " and p.object_key like 'demo-local/%' and p.public_url in " + DEMO_URLS,
            "mapped demo photos", "select count(*)" + DEMO_PHOTO_SCOPE + " and p.object_key like 'demo-local/photo-%' and p.public_url like '/api/dev-demo/images/parts/%.jpg'");

    private DemoOfferPhotoUpdater() { }

    public static void main(String[] args) {
        boolean apply = args.length == 1 && "--apply-demo-photos".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !apply && !"--dry-run".equals(args[0]))) {
            throw new IllegalArgumentException("Use no argument or --dry-run for inspection; pass --apply-demo-photos to explicitly apply.");
        }
        try {
            execute(apply);
        } catch (Exception e) {
            System.err.println("Demo photo update stopped safely: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(2);
        }
    }

    private static void execute(boolean apply) throws Exception {
        String url = requiredEnv("DEMO_DB_URL");
        String username = requiredEnv("DEMO_DB_USERNAME");
        String password = requiredEnv("DEMO_DB_PASSWORD");
        validateDemoJdbcUrl(url);
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(url, properties)) {
            String actualDatabase = scalarString(connection, "select current_database()");
            if (!EXPECTED_DATABASE.equals(actualDatabase)) {
                throw new IllegalStateException("Connected database did not match the required demo database; no changes were made.");
            }
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setReadOnly(!apply);
            connection.setAutoCommit(false);
            try {
                Map<String, Long> before = counts(connection);
                requireExpectedDemoCounts(before);
                List<PhotoRow> candidates = candidates(connection, apply);
                long demoPhotos = before.get("demo offer photos");
                long alreadyMapped = before.get("mapped demo photos");
                long otherDemoPhotos = demoPhotos - candidates.size() - alreadyMapped;
                if (otherDemoPhotos != 0) {
                    throw new IllegalStateException("Demo photo records include an unrecognized URL/key; refusing partial or broad update.");
                }
                if (candidates.isEmpty() && alreadyMapped == 0) {
                    throw new IllegalStateException("No existing demo SVG photo rows or already-mapped rows were found; no changes were made.");
                }

                Map<String, Long> distribution = new LinkedHashMap<>();
                for (PhotoRow row : candidates) {
                    String photoUrl = DemoProductPhotoCatalog.photosForProductName(row.productName()).stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No safe photo mapping for a demo catalog product; no changes were made."));
                    String filename = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
                    row.setTargetUrl(photoUrl);
                    row.setTargetKey("demo-local/photo-" + row.id() + "-" + filename);
                    distribution.merge(filename, 1L, Long::sum);
                }

                System.out.println("DATABASE VERIFIED: " + EXPECTED_DATABASE);
                System.out.println("MODE: " + (apply ? "EXPLICIT APPLY" : "DRY RUN (read-only)"));
                System.out.printf("DEMO COUNTS: users=%d, catalogProducts=%d, offers=%d, offerPhotos=%d%n",
                        before.get("demo users"), before.get("demo catalog products"), before.get("demo offers"), demoPhotos);
                System.out.printf("GLOBAL COUNTS BEFORE: users=%d, catalogProducts=%d, offers=%d, offerPhotos=%d%n",
                        before.get("users"), before.get("catalog products"), before.get("offers"), before.get("offer photos"));
                System.out.printf("PHOTO ROWS: eligibleExistingSvg=%d, alreadyMapped=%d, willUpdate=%d%n",
                        candidates.size(), alreadyMapped, apply ? candidates.size() : 0);
                distribution.forEach((filename, count) -> System.out.println("MAP " + filename + "=" + count));

                if (apply) {
                    int updated = applyUpdates(connection, candidates);
                    if (updated != candidates.size()) throw new IllegalStateException("Updated row count differed from the dry-run plan; rolling back.");
                    Map<String, Long> after = counts(connection);
                    requireCountsUnchanged(before, after);
                    if (after.get("demo SVG photos") != 0 || after.get("mapped demo photos") != demoPhotos) {
                        throw new IllegalStateException("Post-update photo verification failed; rolling back.");
                    }
                    connection.commit();
                    System.out.println("APPLIED AND VERIFIED: updated=" + updated + "; global user/product/offer/photo counts unchanged.");
                } else {
                    connection.rollback();
                    System.out.println("DRY RUN COMPLETE: database transaction rolled back; no data was changed.");
                }
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static List<PhotoRow> candidates(Connection connection, boolean lock) throws SQLException {
        String sql = CANDIDATE_QUERY + (lock ? " for update of p" : "");
        List<PhotoRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) rows.add(new PhotoRow(result.getLong("id"), result.getLong("offer_id"),
                    result.getString("object_key"), result.getString("public_url"), result.getInt("display_order"),
                    result.getBoolean("primary_photo"), result.getString("name")));
        }
        return rows;
    }

    private static int applyUpdates(Connection connection, List<PhotoRow> rows) throws SQLException {
        int updated = 0;
        String sql = "update offer_photos set public_url=?, object_key=? where id=? and offer_id=? and object_key=? and public_url=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PhotoRow row : rows) {
                statement.setString(1, row.targetUrl());
                statement.setString(2, row.targetKey());
                statement.setLong(3, row.id());
                statement.setLong(4, row.offerId());
                statement.setString(5, row.objectKey());
                statement.setString(6, row.publicUrl());
                int rowCount = statement.executeUpdate();
                if (rowCount != 1) throw new IllegalStateException("A planned photo row changed concurrently; rolling back.");
                updated += rowCount;
            }
        }
        return updated;
    }

    private static Map<String, Long> counts(Connection connection) throws SQLException {
        Map<String, Long> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> query : COUNT_QUERIES.entrySet()) {
            values.put(query.getKey(), scalarLong(connection, query.getValue()));
        }
        return values;
    }

    private static void requireExpectedDemoCounts(Map<String, Long> counts) {
        if (counts.get("demo users") != 50 || counts.get("demo catalog products") != 300 || counts.get("demo offers") != 900) {
            throw new IllegalStateException("Demo user/catalog/offer counts were not 50/300/900; refusing to continue.");
        }
    }

    private static void requireCountsUnchanged(Map<String, Long> before, Map<String, Long> after) {
        for (String key : List.of("users", "catalog products", "offers", "offer photos", "demo users", "demo catalog products", "demo offers", "demo offer photos")) {
            if (!before.get(key).equals(after.get(key))) throw new IllegalStateException("Count changed for " + key + "; rolling back.");
        }
    }

    private static long scalarLong(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            result.next(); return result.getLong(1);
        }
    }

    private static String scalarString(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            result.next(); return result.getString(1);
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Required environment variable is not configured: " + name);
        return value;
    }

    static void validateDemoJdbcUrl(String url) {
        if (!url.startsWith("jdbc:postgresql:")) throw new IllegalStateException("Only a PostgreSQL JDBC URL is accepted.");
        URI uri = URI.create(url.substring("jdbc:".length()));
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (!EXPECTED_DATABASE.equals(database)) {
            throw new IllegalStateException("DEMO_DB_URL must target yedekparca_demo_db; no connection was opened.");
        }
    }

    private static final class PhotoRow {
        private final long id;
        private final long offerId;
        private final String objectKey;
        private final String publicUrl;
        @SuppressWarnings("unused") private final int displayOrder;
        @SuppressWarnings("unused") private final boolean primaryPhoto;
        private final String productName;
        private String targetUrl;
        private String targetKey;

        private PhotoRow(long id, long offerId, String objectKey, String publicUrl, int displayOrder, boolean primaryPhoto, String productName) {
            this.id=id; this.offerId=offerId; this.objectKey=objectKey; this.publicUrl=publicUrl;
            this.displayOrder=displayOrder; this.primaryPhoto=primaryPhoto; this.productName=productName;
        }
        private long id() { return id; }
        private long offerId() { return offerId; }
        private String objectKey() { return objectKey; }
        private String publicUrl() { return publicUrl; }
        private String productName() { return productName; }
        private String targetUrl() { return targetUrl; }
        private String targetKey() { return targetKey; }
        private void setTargetUrl(String value) { targetUrl=value; }
        private void setTargetKey(String value) { targetKey=value; }
    }
}
