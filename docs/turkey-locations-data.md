# Türkiye il ve ilçe verisi

Flyway `V9__central_turkey_locations.sql` verisi `sabrigunes/turkey-administrative-units-dataset` deposundaki `csv/cities.csv` ve `csv/districts.csv` dosyalarından alınmıştır. Depo veriyi T.C. İçişleri Bakanlığının e-İçişleri Mülki İdari Bölümleri Envanteri'nden derlediğini ve Ekim 2025 itibarıyla güncel olduğunu belirtir. Depo CC0-1.0 lisansı yayımlar. Kaynak anlık görüntüsü 81 il ve 973 ilçe kayıt satırı içerir; 973 sayısına il merkezi (Merkez) kayıtları da dahildir. Veri 7 Kasım 2026 tarihinde indirildi ve migration içine sabitlendi; uygulamanın çalışması sırasında harici veri servisine çağrı yapılmaz.

Kaynak: https://github.com/sabrigunes/turkey-administrative-units-dataset
Resmî kaynak: https://www.e-icisleri.gov.tr/Anasayfa/MulkiIdariBolumleri.aspx

Legacy `seller_applications.city` ve `district` alanları silinmez. Migration, Türkçe harfleri normalize ederek yalnızca il ve aynı ile bağlı ilçe birlikte eşleştiğinde yeni ID alanlarını doldurur. Eşleşmeyen kayıtların serbest metni korunur ve ID alanları boş kalır; varsayılan/benzer başka bir konuma atanmaz.
