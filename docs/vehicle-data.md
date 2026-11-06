# Merkezi araç veri kataloğu

## Başlangıç veri kümesi ve kapsamı

Seed migration `V8__seed_vehicle_catalog_vehiclesdb_2026_10_3.sql`, VehiclesDB `2026.10.3` sürümündeki `vehicles.csv` verisinden üretilmiştir. Sabitlenmiş kaynak: <https://github.com/vehiclesdb/vehiclesdb/releases/download/v2026.10.3/vehicles.csv>.

Kaynak CSV SHA-256: `D51C2BDD34B0C279CF074E46C8BA9A2EC381DD125678ED8CA0049E129E9F0F3F`.

Kaynakta 15.068 make/model/tür satırı bulunur. Bu migration, adları normalize ederek 937 markaya ve 14.459 marka/model kaydına indirger; tekrar eden marka/model adları tek model kaydı altında toplanır ve kaynakta bulunan tüm araç türü ilişkileri korunur. Araç türü kodları `car`, `van`, `truck`, `bus`, `motorcycle` ve `moped` olarak saklanır. Görünen adları sırasıyla Binek otomobil, Hafif ticari, Kamyon / ticari kamyon, Otobüs, Motosiklet ve Moped'dir.

Bu küme 18 ülkedeki resmî açık kayıt ve kataloglardan derlenmiş küresel bir referans verisidir; Türkiye'ye özel kayıt kaynağı değildir. Bu sürümün model satırlarında Türkiye (`tr`) ülke kanıtı yoktur. Türkiye pazarında kullanıldığı veya eksiksiz olduğu doğrulanmış bir liste olarak sunulamaz. Yeni markalar ve modeller admin API'sinden eklenebilir; veri sağlayıcının yeni sürümleri için sonraki sürümlü seed migration hazırlanmalıdır.

## Lisans ve atıf

VehiclesDB verisi CC BY 4.0 ile sunulur ve görünür, bağlantılı atıf ister. Kullanıcı arayüzünde araç seçimlerinin yanında şu atıf gösterilir: [Vehicle data by VehiclesDB](https://vehiclesdb.com). VehiclesDB'nin upstream kaynakları için zorunlu atıf/lisans bildirimleri [vehicle-data-attribution.md](vehicle-data-attribution.md) dosyasında tutulur; yeni veri sürümü alınırken kaynak bildirimleri de güncellenmelidir.

Kaynak: <https://github.com/vehiclesdb/vehiclesdb> — sürüm `2026.10.3`, CC BY 4.0. Lisans özeti ve upstream bildirimleri: <https://github.com/vehiclesdb/vehiclesdb/blob/v2026.10.3/ATTRIBUTION.md>.
