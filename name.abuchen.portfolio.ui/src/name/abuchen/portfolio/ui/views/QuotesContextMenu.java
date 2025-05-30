package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.datatransfer.csv.CSVExporter;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.SecurityPriceDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.wizards.datatransfer.CSVImportWizard;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportQuotesWizard;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;
import name.abuchen.portfolio.ui.wizards.security.RawResponsesDialog;
import name.abuchen.portfolio.util.QuoteFromTransactionExtractor;
import name.abuchen.portfolio.util.TextUtil;

public class QuotesContextMenu
{
    private AbstractFinanceView owner;

    public QuotesContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager parent, final Security security)
    {
        IMenuManager manager = new MenuManager(Messages.SecurityMenuQuotes);
        parent.add(manager);

        Action action = new Action(Messages.SecurityMenuUpdateQuotes)
        {
            @Override
            public void run()
            {
                new UpdateQuotesJob(owner.getClient(), security).schedule();
            }
        };
        // enable only if online updates are configured
        action.setEnabled(!QuoteFeed.MANUAL.equals(security.getFeed())
                        || (security.getLatestFeed() != null && !QuoteFeed.MANUAL.equals(security.getLatestFeed())));
        manager.add(action);

        action = new SimpleAction(Messages.SecurityMenuDebugGetHistoricalQuotes, a -> {
            try
            {
                new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, m -> {

                    if (QuoteFeed.MANUAL.equals(security.getFeed()))
                        return;

                    QuoteFeed feed = Factory.getQuoteFeedProvider(security.getFeed());
                    if (feed == null)
                        return;

                    QuoteFeedData data;
                    try
                    {
                        data = feed.getHistoricalQuotes(security, true);
                    }
                    catch (QuoteFeedException e)
                    {
                        data = QuoteFeedData.withError(e);
                    }

                    PortfolioPlugin.log(data.getErrors());

                    var feedData = data;
                    Display.getDefault().asyncExec(() -> {

                        if (!feedData.getResponses().isEmpty() || feedData.getErrors().isEmpty())
                        {
                            new RawResponsesDialog(Display.getDefault().getActiveShell(), feedData.getResponses()).open();
                        }
                        else
                        {
                            MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR,
                                            security.getName(), null);
                            feedData.getErrors().forEach(e -> status
                                            .add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage())));
                            ErrorDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                            security.getName(), status);
                        }
                    });
                });
            }
            catch (InvocationTargetException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                e.getCause().getMessage());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        action.setEnabled(!QuoteFeed.MANUAL.equals(security.getFeed()));
        manager.add(action);

        manager.add(new Action(Messages.SecurityMenuConfigureOnlineUpdate)
        {
            @Override
            public void run()
            {
                EditSecurityDialog dialog = owner.make(EditSecurityDialog.class, security);
                dialog.setShowQuoteConfigurationInitially(true);

                if (dialog.open() != Window.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new SimpleAction(Messages.LabelSearchForQuoteFeeds + "...", //$NON-NLS-1$
                        a -> Display.getDefault().asyncExec(() -> {
                            FindQuoteProviderDialog dialog = new FindQuoteProviderDialog(
                                            Display.getDefault().getActiveShell(), owner.getClient(),
                                            List.of(security));
                            dialog.open();
                        })));

        manager.add(new Separator());

        manager.add(new Action(Messages.SecurityMenuImportCSV)
        {
            @Override
            public void run()
            {
                FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
                fileDialog.setFilterNames(
                                new String[] { Messages.CSVImportLabelFileCSV, Messages.CSVImportLabelFileAll });
                fileDialog.setFilterExtensions(new String[] { "*.csv;*.CSV", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
                String fileName = fileDialog.open();

                if (fileName == null)
                    return;

                CSVImportWizard wizard = new CSVImportWizard(owner.getClient(), owner.getPreferenceStore(),
                                new File(fileName));
                owner.inject(wizard);
                wizard.setTarget(security);
                Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);

                if (dialog.open() != Window.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuImportHTML)
        {
            @Override
            public void run()
            {
                Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(),
                                new ImportQuotesWizard(security));

                if (dialog.open() != Window.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuCreateManually)
        {
            @Override
            public void run()
            {
                Dialog dialog = new SecurityPriceDialog(Display.getDefault().getActiveShell(), owner.getClient(),
                                security);

                if (dialog.open() != Window.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Separator());

        manager.add(new Action(Messages.SecurityMenuExportCSV)
        {
            @Override
            public void run()
            {
                FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
                fileDialog.setFilterNames(new String[] { Messages.CSVImportLabelFileCSV });
                fileDialog.setFilterExtensions(new String[] { "*.csv" }); //$NON-NLS-1$

                fileDialog.setFileName(TextUtil.sanitizeFilename(security.getName() + ".csv")); //$NON-NLS-1$
                fileDialog.setOverwrite(true);
                String fileName = fileDialog.open();

                if (fileName == null)
                    return;

                try
                {
                    new CSVExporter().exportSecurityPrices(new File(fileName), security);
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                    MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
                }
            }
        });

        manager.add(new Separator());

        if (security.getCurrencyCode() != null)
        {
            manager.add(new Action(Messages.SecurityMenuCreateQuotesFromTransactions)
            {
                @Override
                public void run()
                {
                    ExchangeRateProviderFactory factory = owner.getFromContext(ExchangeRateProviderFactory.class);
                    CurrencyConverter converter = new CurrencyConverterImpl(factory, security.getCurrencyCode());
                    QuoteFromTransactionExtractor qte = new QuoteFromTransactionExtractor(owner.getClient(), converter);
                    if (qte.extractQuotes(security))
                    {
                        owner.markDirty();
                        owner.notifyModelUpdated();
                    }
                }
            });
        }

        if (security.getLatest() != null)
        {
            manager.add(new SimpleAction(Messages.SecurityMenuDeleteLatestQuote, a -> {
                security.setLatest(null);
                owner.markDirty();
                owner.notifyModelUpdated();
            }));
        }

        manager.add(new SimpleAction(Messages.SecurityMenuRoundToXDecimalPlaces, a -> {

            IInputValidator validator = newText -> {
                try
                {
                    int decimalPlaces = Integer.parseInt(newText);

                    if (decimalPlaces < 0 || decimalPlaces > Values.Quote.precision())
                        return MessageFormat.format(Messages.SecurityMenuErrorMessageRoundingMustBeBetween0AndX,
                                        Values.Quote.precision());

                    return null;
                }
                catch (NumberFormatException e)
                {
                    return String.format(Messages.CellEditor_NotANumber, newText);
                }
            };

            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
                            Messages.SecurityMenuRoundToXDecimalPlaces, Messages.SecurityMenuLabelNumberOfDecimalPlaces,
                            String.valueOf(4), validator);

            if (dialog.open() != Window.OK)
                return;

            int newPrecision = Integer.parseInt(dialog.getValue());

            boolean isDirty = false;

            for (SecurityPrice price : security.getPrices())
            {
                final long oldValue = price.getValue();
                final long newValue = BigDecimal.valueOf(oldValue).movePointLeft(Values.Quote.precision())
                                .setScale(newPrecision, Values.MC.getRoundingMode())
                                .movePointRight(Values.Quote.precision()).longValue();

                if (oldValue != newValue)
                {
                    price.setValue(newValue);
                    isDirty = true;
                }
            }

            if (isDirty)
            {
                owner.markDirty();
                owner.notifyModelUpdated();
            }
        }));
    }
}
