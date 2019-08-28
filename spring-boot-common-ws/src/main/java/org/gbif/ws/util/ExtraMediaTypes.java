package org.gbif.ws.util;

import org.springframework.http.MediaType;

/**
 * Extra media types used in Http responses.
 */
public final class ExtraMediaTypes {

  public static final String APPLICATION_JAVASCRIPT = "application/javascript";
  public static final MediaType APPLICATION_JAVASCRIPT_TYPE = new MediaType("application", "javascript");

  public static final String TEXT_CSV = "text/csv";
  public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

  public static final String TEXT_TSV = "text/tab-separated-values";
  public static final MediaType TEXT_TSV_TYPE = new MediaType("text", "tab-separated-values");

  //.xls
  public static final String APPLICATION_EXCEL = "application/vnd.ms-excel";
  public static final MediaType APPLICATION_EXCEL_TYPE = new MediaType("application", "vnd.ms-excel");

  //.xlsx
  public static final String APPLICATION_OFFICE_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final MediaType APPLICATION_OFFICE_SPREADSHEET_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  //.ods
  public static final String APPLICATION_OPEN_DOC_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
  public static final MediaType APPLICATION_OPEN_DOC_SPREADSHEET_TYPE = new MediaType("application", "vnd.oasis.opendocument.spreadsheet");

  //the common one is defined by com.sun.jersey.multipart.file.CommonMediaTypes.ZIP , this is another used by some sites
  public static final String APPLICATION_XZIP_COMPRESSED = "application/x-zip-compressed";
  public static final MediaType APPLICATION_XZIP_COMPRESSED_TYPE = new MediaType("application", "x-zip-compressed");

  public static final String APPLICATION_GZIP = "application/gzip";
  public static final MediaType APPLICATION_GZIP_TYPE = new MediaType("application", "gzip");

  /**
   * Darwin Core archive media type with underlying zip structure.
   * Use carefully, it's an unregistered media type, in most of the cases it is more appropriate to return a simple
   * application/zip
   * http://www.iana.org/assignments/media-types/media-types.xhtml
   * Currently used for experimenting in OAI-PMH DublinCore resources.
   */
  public static final String APPLICATION_DWCA = "application/dwca+zip";

  private ExtraMediaTypes() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
