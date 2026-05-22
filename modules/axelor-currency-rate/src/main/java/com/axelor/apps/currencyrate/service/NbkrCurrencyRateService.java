package com.axelor.apps.currencyrate.service;

import com.axelor.apps.currencyrate.db.CurrencyRate;
import com.axelor.apps.currencyrate.db.repo.CurrencyRateRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import jakarta.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


@Singleton
public class NbkrCurrencyRateService {

  private static final Logger LOG = LoggerFactory.getLogger(NbkrCurrencyRateService.class);

  private static final String NBKR_URL = "https://www.nbkr.kg/XML/daily.xml";

  private static final DateTimeFormatter NBKR_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy");

  /** Human-readable names for the most common currencies (fallback to ISO code). */
  private static final Map<String, String> CURRENCY_NAMES =
      Map.ofEntries(
          Map.entry("USD", "US Dollar"),
          Map.entry("EUR", "Euro"),
          Map.entry("RUB", "Russian Ruble"),
          Map.entry("KZT", "Kazakh Tenge"),
          Map.entry("CNY", "Chinese Yuan"),
          Map.entry("GBP", "British Pound"),
          Map.entry("JPY", "Japanese Yen"),
          Map.entry("TRY", "Turkish Lira"),
          Map.entry("UZS", "Uzbek Som"),
          Map.entry("KGS", "Kyrgyz Som"));

  protected final CurrencyRateRepository repository;

  @Inject
  public NbkrCurrencyRateService(CurrencyRateRepository repository) {
    this.repository = repository;
  }


  @Transactional
  public int syncDailyRates() {
    LOG.info("NBKR sync started");
    try {
      byte[] xml = fetchXml();
      Document doc = parseXml(xml);
      Element root = doc.getDocumentElement();

      LocalDate rateDate = LocalDate.parse(root.getAttribute("Date"), NBKR_DATE_FORMAT);
      LOG.info("NBKR date: {}", rateDate);

      int count = 0;
      NodeList list = root.getElementsByTagName("Currency");
      for (int i = 0; i < list.getLength(); i++) {
        Element c = (Element) list.item(i);
        String code = c.getAttribute("ISOCode");
        int nominal = Integer.parseInt(textOf(c, "Nominal"));
        BigDecimal rate = parseRate(textOf(c, "Value"));
        upsert(code, nominal, rate, rateDate);
        count++;
      }

      upsert("KGS", 1, BigDecimal.ONE, rateDate);
      count++;

      LOG.info("NBKR sync finished: {} rates for {}", count, rateDate);
      return count;
    } catch (Exception e) {
      LOG.error("NBKR sync failed", e);
      throw new RuntimeException("NBKR sync failed: " + e.getMessage(), e);
    }
  }

  protected byte[] fetchXml() throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(NBKR_URL))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
    HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    if (resp.statusCode() != 200) {
      throw new RuntimeException("NBKR returned HTTP " + resp.statusCode());
    }
    return resp.body();
  }

  protected Document parseXml(byte[] xml) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder b = f.newDocumentBuilder();
    return b.parse(new ByteArrayInputStream(xml));
  }

  protected String textOf(Element parent, String tag) {
    return parent.getElementsByTagName(tag).item(0).getTextContent().trim();
  }

  protected BigDecimal parseRate(String value) {
    return new BigDecimal(value.replace(',', '.'));
  }

  protected void upsert(String code, int nominal, BigDecimal rate, LocalDate date) {
    CurrencyRate row = repository.findByCodeAndDate(code, date);
    if (row == null) {
      row = new CurrencyRate();
      row.setCode(code);
      row.setRateDate(date);
    }
    row.setName(CURRENCY_NAMES.getOrDefault(code, code));
    row.setNominal(nominal);
    row.setRate(rate);
    repository.save(row);
    LOG.debug("Upsert {} = {} (nominal {}) for {}", code, rate, nominal, date);
  }
}
