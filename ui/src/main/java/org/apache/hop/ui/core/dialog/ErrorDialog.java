/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.ui.core.dialog;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hop.core.Const;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

/**
 * Dialog to display an error generated by a Hop Exception.
 *
 * @author Matt
 * @since 19-06-2003
 */
public class ErrorDialog extends Dialog {
  private static final Class<?> PKG = ErrorDialog.class; // Needed by Translator

  private Label wlDesc;
  private Text wDesc;
  private FormData fdlDesc, fdDesc;

  private Button wOk, wDetails, wCancel;

  private Shell shell;
  private SelectionAdapter lsDef;
  private PropsUi props;

  private boolean cancelled;
  private Function<String, String> exMsgFunction = Function.identity();

  // private ILogChannel log;

  public ErrorDialog( Shell parent, String title, String message, Throwable throwable ) {
    this( parent, title, message, throwable, Function.identity() );
  }

  public ErrorDialog( Shell parent, String title, String message, Throwable throwable, Function<String, String> exMsgFunction ) {
    super( parent, SWT.NONE );
    this.exMsgFunction = exMsgFunction;

    throwable.printStackTrace();

    // this.log = new LogChannel("ErrorDialog");
    // log.logError(message, throwable);

    if ( throwable instanceof Exception ) {
      showErrorDialog( parent, title, message, (Exception) throwable, false );
    } else {
      // not optimal, but better then nothing
      showErrorDialog( parent, title, message + Const.CR + Const.getStackTracker( throwable ), null, false );
    }
  }

  public ErrorDialog( Shell parent, String title, String message, Exception exception ) {
    super( parent, SWT.NONE );
    showErrorDialog( parent, title, message, exception, false );
  }

  public ErrorDialog( Shell parent, String title, String message, Exception exception, boolean showCancelButton ) {
    super( parent, SWT.NONE );
    showErrorDialog( parent, title, message, exception, showCancelButton );
  }

  private void showErrorDialog( Shell parent, String title, String message, Exception exception,
                                boolean showCancelButton ) {
    if ( parent.isDisposed() ) {
      exception.printStackTrace();
      return;
    }
    this.props = PropsUi.getInstance();

    Display display = parent.getDisplay();
    final Font largeFont = GuiResource.getInstance().getFontBold();
    final Color gray = GuiResource.getInstance().getColorDemoGray();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.APPLICATION_MODAL | SWT.SHEET );
    props.setLook( shell );
    shell.setImage( GuiResource.getInstance().getImageShowErrorLines() );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( title );

    int margin = props.getMargin();

    // From transform line
    wlDesc = new Label( shell, SWT.NONE );
    wlDesc.setText( message );
    props.setLook( wlDesc );
    fdlDesc = new FormData();
    fdlDesc.left = new FormAttachment( 0, 0 );
    fdlDesc.top = new FormAttachment( 0, margin );
    wlDesc.setLayoutData( fdlDesc );
    wlDesc.setFont( largeFont );

    wDesc = new Text( shell, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );

    final StringBuilder text = new StringBuilder();
    final StringBuilder details = new StringBuilder();

    if ( exception != null ) {
      handleException( message, exception, text, details );
      wDesc.setText( exMsgFunction.apply( text.toString() ) );
    } else {
      text.append( message );
      wDesc.setText( exMsgFunction.apply( text.toString() ) );
    }
    wDesc.setBackground( gray );
    fdDesc = new FormData();
    fdDesc.left = new FormAttachment( 0, 0 );
    fdDesc.top = new FormAttachment( wlDesc, margin );
    fdDesc.right = new FormAttachment( 100, 0 );
    fdDesc.bottom = new FormAttachment( 100, -50 );
    wDesc.setLayoutData( fdDesc );
    wDesc.setEditable( false );

    wOk = new Button( shell, SWT.PUSH );
    wOk.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    if ( showCancelButton ) {
      wCancel = new Button( shell, SWT.PUSH );
      wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    }
    wDetails = new Button( shell, SWT.PUSH );
    wDetails.setText( BaseMessages.getString( PKG, "System.Button.Details" ) );

    Button[] buttons;
    if ( showCancelButton ) {
      buttons = new Button[] { wOk, wCancel, wDetails, };
    } else {
      buttons = new Button[] { wOk, wDetails, };
    }

    BaseTransformDialog.positionBottomButtons( shell, buttons, margin, null );

    // Add listeners
    wOk.addListener( SWT.Selection, e -> ok() );
    if ( showCancelButton ) {
      wCancel.addListener( SWT.Selection, e -> cancel() );
    }
    wDetails.addListener( SWT.Selection, e -> showDetails( details.toString() ) );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };
    wDesc.addSelectionListener( lsDef );

    // Detect [X] or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        ok();
      }
    } );
    // Clean up used resources!
    shell.addDisposeListener( arg0 -> {
    } );

    BaseTransformDialog.setSize( shell );

    // Set the focus on the "OK" button
    wOk.setFocus();

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
  }

  @VisibleForTesting
  protected void handleException( String message, Exception exception, StringBuilder text, StringBuilder details ) {
    text.append(Const.getSimpleStackTrace( exception ));
    text.append(Const.CR);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    exception.printStackTrace( pw );

    details.append( sw.getBuffer() );
  }

  protected void showDetails( String details ) {
    EnterTextDialog dialog = new EnterTextDialog( shell,
      BaseMessages.getString( PKG, "ErrorDialog.ShowDetails.Title" ),
      BaseMessages.getString( PKG, "ErrorDialog.ShowDetails.Message" ), details );
    dialog.setReadOnly();
    dialog.open();
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  private void ok() {
    dispose();
  }

  private void cancel() {
    cancelled = true;
    dispose();
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
