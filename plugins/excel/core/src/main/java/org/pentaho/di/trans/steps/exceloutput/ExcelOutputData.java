/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.exceloutput;

import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import jxl.WorkbookSettings;
import jxl.format.Colour;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableImage;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Matt
 * @since 7-sep-2006
 */
public class ExcelOutputData extends BaseStepData implements StepDataInterface {
  public int splitnr;

  public RowMetaInterface previousMeta;
  public RowMetaInterface outputMeta;
  public int[] fieldnrs;

  public WritableWorkbook workbook;

  public WritableSheet sheet;

  public int templateColumns; // initial number of columns in the template

  public WritableCellFormat writableCellFormat;

  public Map<String, WritableCellFormat> formats;

  public int positionX;

  public int positionY;

  public WritableFont headerFont;

  public OutputStream outputStream;

  public FileObject file;

  public boolean oneFileOpened;

  public String realSheetname;

  int[] fieldsWidth;

  public boolean headerWrote;

  public int Headerrowheight;

  public String realHeaderImage;

  public Colour rowFontBackgoundColour;

  public WritableCellFormat headerCellFormat;

  public WritableImage headerImage;

  public double headerImageHeight;
  public double headerImageWidth;
  public WritableFont writableFont;

  public String realFilename;

  public WorkbookSettings ws;

  public ExcelOutputData() {
    super();

    formats = new Hashtable<String, WritableCellFormat>();
    oneFileOpened = false;
    file = null;
    realSheetname = null;
    headerWrote = false;
  }

}
