package name.abuchen.portfolio.datatransfer.pdf.swissquote;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SwissquotePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class SwissquotePDFExtractorTest
{
    SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testWertpapierKauf01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("APPLE ORD"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Referenz: 32484929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("USD", Values.Amount.factorize(2900.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("USD", Values.Amount.factorize(2895.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("USD", Values.Amount.factorize(4.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("USD", Values.Amount.factorize(0.85))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0001752309"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("FISCHER N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Referenz: 32484929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2747.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2713.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(2.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(31.85))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DK0010268606"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VESTAS WIND SYSTEMS ORD"));
        assertThat(security.getCurrencyCode(), is("DKK"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-12T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz: 32484929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(5650.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(5602.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(8.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(39.10))));

        var grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("DKK", Values.Amount.factorize(37301.50))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("VESTAS WIND SYSTEMS ORD", "CHF");
        security.setIsin("DK0010268606");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-12T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz: 32484929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(5650.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(5602.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(8.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(39.10))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("CHF");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3RBWM25"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard All World ETF Dist"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Referenz: 206871550"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2102.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2087.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(3.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(9.85 + 2.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0210483332"));
        assertThat(security.getName(), is("RICHEMONT N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-10-31T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.0269)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Referenz: 312345678"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(100.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(99.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("QQQ MAY24 460C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-25T00:00"), hasShares(500.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Referenz: 539850276"), //
                        hasAmount("USD", 3144.75), hasGrossValue("USD", 3135.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 2.25 + 7.50))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-29T00:00"), hasShares(300.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Referenz: 581899025"), //
                        hasAmount("USD", 3579.35), hasGrossValue("USD", 3573.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.35 + 5.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 530C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-23T00:00"), hasShares(500.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Referenz: 578365281"), //
                        hasAmount("USD", 4769.75), hasGrossValue("USD", 4760.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 2.25 + 7.50))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn(null), hasTicker(null), //
                        hasName("AMAZON COM ORD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-15T00:00"), hasShares(0.4683), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Referenz: 699111111"), //
                        hasAmount("USD", 95.00), hasGrossValue("USD", 94.86), //
                        hasTaxes("USD", 0.14), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecurityBuy01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World UCITS ETF USD"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-06T00:00"), hasShares(3.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Ref.-Nr.: 123456789"), //
                        hasAmount("CHF", 390.45), hasGrossValue("CHF", 387.85), //
                        hasTaxes("CHF", 0.60), hasFees("CHF", 2.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0363463438"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("IDORSIA N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-02-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(322)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Referenz: 32484929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(8198.70))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(8236.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(6.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(31.85))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DK0010268606"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VESTAS WIND SYSTEMS ORD"));
        assertThat(security.getCurrencyCode(), is("DKK"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-03-08T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Referenz: 32474929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(5267.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(5305.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(31.85))));

        var grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("DKK", Values.Amount.factorize(35410.50))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInCHF()
    {
        var security = new Security("VESTAS WIND SYSTEMS ORD", "CHF");
        security.setIsin("DK0010268606");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-03-08T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Referenz: 32474929"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(5267.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(5305.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(31.85))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("CHF");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-07T00:00"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Referenz: XXXXXX"), //
                        hasAmount("USD", 1159.55), hasGrossValue("USD", 1160.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.45))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 530C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-24T00:00"), hasShares(200.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Referenz: 579076719"), //
                        hasAmount("USD", 2348.10), hasGrossValue("USD", 2350.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.90 + 1.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-31T00:00"), hasShares(300.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Referenz: 583488434"), //
                        hasAmount("USD", 3461.65), hasGrossValue("USD", 3468.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.35 + 5.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("NVDA AUG24 1,130P"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-04T00:00"), hasShares(100.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Referenz: 585697505"), //
                        hasAmount("USD", 7044.55), hasGrossValue("USD", 7050.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.45 + 5.00))));
    }

    @Test
    public void testExpiryOption01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OptionVerfall01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("QQQ APR24 447C"), //
                        hasCurrencyCode("USD"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2024-04-08T00:00"), hasShares(100.00), //
                                        hasSource("OptionVerfall01.txt"), //
                                        hasNote("Referenz: 549183576"), //
                                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));
    }

    @Test
    public void testCryptoKauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("USD"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-19T00:00"), hasShares(0.02), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Referenz: 535993271"), //
                        hasAmount("USD", 1285.61), hasGrossValue("USD", 1272.88), //
                        hasTaxes("USD", 0.00), hasFees("USD", 12.73))));
    }

    @Test
    public void testDividende01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US41753F1093"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HARVEST CAPITAL CREDIT ORD"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(350)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Referenz: 32484929"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("USD", Values.Amount.factorize(19.60))));
        assertThat(transaction.getGrossValue(), is(Money.of("USD", Values.Amount.factorize(28.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(8.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("USD", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        var security = new Security("HARVEST CAPITAL CREDIT ORD", "CHF");
        security.setIsin("US41753F1093");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-27T00:00"), hasShares(350.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("USD", 19.60), hasGrossValue("USD", 28.00), //
                        hasForexGrossValue("CHF", 27.37), //
                        hasTaxes("USD", 8.40), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0025751329"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LOGITECH N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Referenz: 32484929"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(118.62))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(182.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(63.88))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3RBWM25"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard All World ETF Dist"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(40.00)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Referenz: 222443221"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("USD", Values.Amount.factorize(13.52))));
        assertThat(transaction.getGrossValue(), is(Money.of("USD", Values.Amount.factorize(13.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("USD", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03WithSecurityInCHF()
    {
        var security = new Security("Vanguard All World ETF Dist", "CHF");
        security.setIsin("IE00B3RBWM25");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-31T00:00"), hasShares(40.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Referenz: 222443221"), //
                        hasAmount("USD", 13.52), hasGrossValue("USD", 13.52), //
                        hasForexGrossValue("CHF", 12.74), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0371153492"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Landis+Gyr N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Referenz: 32484929"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(107.10))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(107.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0012032048"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ROCHE GS"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4.0542)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Referenz: 312345678"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.03))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(38.51))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(13.48))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B8GKDB10"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard AllWrld Div ETF Dist"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.553)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Referenz: 123456789"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("USD", Values.Amount.factorize(1.46))));
        assertThat(transaction.getGrossValue(), is(Money.of("USD", Values.Amount.factorize(1.46))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("USD", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06WithSecurityInCHF()
    {
        var security = new Security("Vanguard AllWrld Div ETF Dist", "CHF");
        security.setIsin("IE00B8GKDB10");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-29T00:00"), hasShares(3.553), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Referenz: 123456789"), //
                        hasAmount("USD", 1.46), hasGrossValue("USD", 1.46), //
                        hasForexGrossValue("CHF", 1.34), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0009690239"), hasWkn(null), hasTicker(null), //
                        hasName("VanEck Global Real Estate ETF"), //
                        hasCurrencyCode("EUR"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-13T00:00"), hasShares(420.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Referenz: 548895976"), //
                        hasAmount("EUR", 114.24), hasGrossValue("EUR", 134.40), //
                        hasTaxes("EUR", 20.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInCHF()
    {
        var security = new Security("VanEck Global Real Estate ETF", "CHF");
        security.setIsin("NL0009690239");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-13T00:00"), hasShares(420.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Referenz: 548895976"), //
                        hasAmount("EUR", 114.24), hasGrossValue("EUR", 134.40), //
                        hasForexGrossValue("CHF", 127.03), //
                        hasTaxes("EUR", 20.16), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US82889N6739"), hasWkn(null), hasTicker(null), //
                        hasName("SIMPLIFY BITCOIN STGY INC ETF"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-02-28T00:00"), hasShares(281.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Referenz: 795604930"), //
                        hasAmount("USD", 23.88), hasGrossValue("USD", 28.10), //
                        hasTaxes("USD", 4.22), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInCHF()
    {
        var security = new Security("SIMPLIFY BITCOIN STGY INC ETF", "CHF");
        security.setIsin("US82889N6739");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-02-28T00:00"), hasShares(281.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Referenz: 795604930"), //
                        hasAmount("USD", 23.88), hasGrossValue("USD", 28.10), //
                        hasForexGrossValue("CHF", 25.28), //
                        hasTaxes("USD", 4.22), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US37954Y4594"), hasWkn(null), hasTicker(null), //
                        hasName("GLOBAL X RUSSELL 2000 CVRED CALL ET F"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasShares(300.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Referenz: 532146499"), //
                        hasAmount("USD", 41.49), hasGrossValue("USD", 48.81), //
                        hasTaxes("USD", 7.32), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInCHF()
    {
        var security = new Security("GLOBAL X RUSSELL 2000 CVRED CALL ET F", "CHF");
        security.setIsin("US37954Y4594");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasShares(300.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Referenz: 532146499"), //
                        hasAmount("USD", 41.49), hasGrossValue("USD", 48.81), //
                        hasForexGrossValue("CHF", 44.05), //
                        hasTaxes("USD", 7.32), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US87612E1064"), hasWkn(null), hasTicker(null), //
                        hasName("TARGET ORD"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasShares(8.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Referenz: 055857024"), //
                        hasAmount("USD", 6.28), hasGrossValue("USD", 8.96), //
                        hasTaxes("USD", 2.68), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInCHF()
    {
        var security = new Security("TARGET ORD", "CHF");
        security.setIsin("US87612E1064");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasShares(8.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Referenz: 055857024"), //
                        hasAmount("USD", 6.28), hasGrossValue("USD", 8.96), //
                        hasForexGrossValue("CHF", 8.09), //
                        hasTaxes("USD", 2.68), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasWkn(null), hasTicker(null), //
                        hasName("META PLATFORMS CL A ORD"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-26T00:00"), hasShares(29.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Referenz: 819926315"), //
                        hasAmount("USD", 10.67), hasGrossValue("USD", 15.23), //
                        hasTaxes("USD", 2.28 + 2.28), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInCHF()
    {
        var security = new Security("META PLATFORMS CL A ORD", "CHF");
        security.setIsin("US30303M1027");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-26T00:00"), hasShares(29.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Referenz: 819926315"), //
                        hasForexGrossValue("CHF", 13.46), //
                        hasTaxes("USD", 2.28 + 2.28), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0015811955"), hasWkn(null), hasTicker(null), //
                        hasName("INVESTOR ORD"), //
                        hasCurrencyCode("SEK"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-14T00:00"), hasShares(175.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Referenz: 847092627"), //
                        hasAmount("CHF", 38.87), hasGrossValue("CHF", 55.53), //
                        hasForexGrossValue("SEK", 656.25), //
                        hasTaxes("CHF", 16.66), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInCHF()
    {
        var security = new Security("INVESTOR ORD", "CHF");
        security.setIsin("SE0015811955");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-14T00:00"), hasShares(175.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Referenz: 847092627"), //
                        hasAmount("CHF", 38.87), hasGrossValue("CHF", 55.53), //
                        hasTaxes("CHF", 16.66), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testZahlungsverkehr01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Zahlungsverkehr01.txt"));
        assertThat(transaction.getNote(), is("Referenz: 312345678"));
    }

    @Test
    public void testZahlungsverkehr02()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Zahlungsverkehr02.txt"));
        assertThat(transaction.getNote(), is("Referenz: 123456789"));
    }

    @Test
    public void testZahlungsverkehr03()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(11.00))));
        assertThat(transaction.getSource(), is("Zahlungsverkehr03.txt"));
        assertThat(transaction.getNote(), is("Referenz: 123456789"));
    }

    @Test
    public void testZahlungsverkehr04()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(605.66))));
        assertThat(transaction.getSource(), is("Zahlungsverkehr04.txt"));
        assertThat(transaction.getNote(), is("Referenz: 567891234"));
    }

    @Test
    public void testZahlungsverkehr05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fee transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-30T00:00")));
        assertThat(transaction.getSource(), is("Zahlungsverkehr05.txt"));
        assertThat(transaction.getNote(), is("Referenz: 32484929 | Depotgebühren"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(28.55))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(28.55))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testZinsabrechnung01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        // We have three currency transactions
        // new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.36))));
        assertThat(transaction.getSource(), is("Zinsabrechnung01.txt"));
        assertThat(transaction.getNote(), is("Zinsabrechnung 05.09.2022 - 31.12.2022"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("USD", Values.Amount.factorize(0.07))));
        assertThat(transaction.getSource(), is("Zinsabrechnung01.txt"));
        assertThat(transaction.getNote(), is("Zinsabrechnung 05.09.2022 - 31.12.2022"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.59))));
        assertThat(transaction.getSource(), is("Zinsabrechnung01.txt"));
        assertThat(transaction.getNote(), is("Zinsabrechnung 05.09.2022 - 31.12.2022"));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-03-31"), hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Depotgebühren"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-06-30"), hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Depotgebühren"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-09-29"), hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Depotgebühren"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-29"), hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Depotgebühren"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2023-12-31"), hasAmount("CHF", 127.85), //
                        hasSource("Kontoauszug01.txt"), hasNote("Sollzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2023-12-31"), hasAmount("EUR", 18.56), //
                        hasSource("Kontoauszug01.txt"), hasNote("Sollzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2023-12-31"), hasAmount("USD", 41.39), //
                        hasSource("Kontoauszug01.txt"), hasNote("Sollzinsen"))));
    }
}
