package com.example.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.nio.ByteBuffer;

import jpos.CashDrawer;
import jpos.JposConst;
import jpos.JposException;
import jpos.LocalSmartCardRW;
import jpos.MSR;
import jpos.MSRConst;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.SmartCardRW;
import jpos.SmartCardRWConst;
import jpos.config.JposEntry;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

import com.bxl.config.editor.BXLConfigLoader;

public class BixolonPrinter {
    // ------------------- alignment ------------------- //
    public static int ALIGNMENT_LEFT = 1;
    public static int ALIGNMENT_CENTER = 2;
    public static int ALIGNMENT_RIGHT = 4;

    // ------------------- Text attribute ------------------- //
    public static int ATTRIBUTE_NORMAL = 0;
    public static int ATTRIBUTE_FONT_A = 1;
    public static int ATTRIBUTE_FONT_B = 2;
    public static int ATTRIBUTE_FONT_C = 4;
    public static int ATTRIBUTE_BOLD = 8;
    public static int ATTRIBUTE_UNDERLINE = 16;
    public static int ATTRIBUTE_REVERSE = 32;
    public static int ATTRIBUTE_FONT_D = 64;

    // ------------------- Barcode Symbology ------------------- //
    public static int BARCODE_TYPE_UPCA = POSPrinterConst.PTR_BCS_UPCA;
    public static int BARCODE_TYPE_UPCE = POSPrinterConst.PTR_BCS_UPCE;
    public static int BARCODE_TYPE_EAN8 = POSPrinterConst.PTR_BCS_EAN8;
    public static int BARCODE_TYPE_EAN13 = POSPrinterConst.PTR_BCS_EAN13;
    public static int BARCODE_TYPE_ITF = POSPrinterConst.PTR_BCS_ITF;
    public static int BARCODE_TYPE_Codabar = POSPrinterConst.PTR_BCS_Codabar;
    public static int BARCODE_TYPE_Code39 = POSPrinterConst.PTR_BCS_Code39;
    public static int BARCODE_TYPE_Code93 = POSPrinterConst.PTR_BCS_Code93;
    public static int BARCODE_TYPE_Code128 = POSPrinterConst.PTR_BCS_Code128;
    public static int BARCODE_TYPE_PDF417 = POSPrinterConst.PTR_BCS_PDF417;
    public static int BARCODE_TYPE_MAXICODE = POSPrinterConst.PTR_BCS_MAXICODE;
    public static int BARCODE_TYPE_DATAMATRIX = POSPrinterConst.PTR_BCS_DATAMATRIX;
    public static int BARCODE_TYPE_QRCODE = POSPrinterConst.PTR_BCS_QRCODE;
    public static int BARCODE_TYPE_EAN128 = POSPrinterConst.PTR_BCS_EAN128;

    // ------------------- Barcode HRI ------------------- //
    public static int BARCODE_HRI_NONE = POSPrinterConst.PTR_BC_TEXT_NONE;
    public static int BARCODE_HRI_ABOVE = POSPrinterConst.PTR_BC_TEXT_ABOVE;
    public static int BARCODE_HRI_BELOW = POSPrinterConst.PTR_BC_TEXT_BELOW;

    // ------------------- Farsi Option ------------------- //
    public static int OPT_REORDER_FARSI_RTL = 0;
    public static int OPT_REORDER_FARSI_MIXED = 1;

    // ------------------- CharacterSet ------------------- //
    public static int CS_437_USA_STANDARD_EUROPE = 437;
    public static int CS_737_GREEK = 737;
    public static int CS_775_BALTIC = 775;
    public static int CS_850_MULTILINGUAL = 850;
    public static int CS_852_LATIN2 = 852;
    public static int CS_855_CYRILLIC = 855;
    public static int CS_857_TURKISH = 857;
    public static int CS_858_EURO = 858;
    public static int CS_860_PORTUGUESE = 860;
    public static int CS_862_HEBREW_DOS_CODE = 862;
    public static int CS_863_CANADIAN_FRENCH = 863;
    public static int CS_864_ARABIC = 864;
    public static int CS_865_NORDIC = 865;
    public static int CS_866_CYRILLIC2 = 866;
    public static int CS_928_GREEK = 928;
    public static int CS_1250_CZECH = 1250;
    public static int CS_1251_CYRILLIC = 1251;
    public static int CS_1252_LATIN1 = 1252;
    public static int CS_1253_GREEK = 1253;
    public static int CS_1254_TURKISH = 1254;
    public static int CS_1255_HEBREW_NEW_CODE = 1255;
    public static int CS_1256_ARABIC = 1256;
    public static int CS_1257_BALTIC = 1257;
    public static int CS_1258_VIETNAM = 1258;
    public static int CS_FARSI = 7065;
    public static int CS_KATAKANA = 7565;
    public static int CS_KHMER_CAMBODIA = 7572;
    public static int CS_THAI11 = 8411;
    public static int CS_THAI14 = 8414;
    public static int CS_THAI16 = 8416;
    public static int CS_THAI18 = 8418;
    public static int CS_THAI42 = 8442;
    public static int CS_KS5601 = 5601;
    public static int CS_BIG5 = 6605;
    public static int CS_GB2312 = 2312;
    public static int CS_SHIFT_JIS = 8374;
    public static int CS_TCVN_3_1 = 3031;
    public static int CS_TCVN_3_2 = 3032;

    private Context context = null;

    private BXLConfigLoader bxlConfigLoader = null;
    public POSPrinter posPrinter = null;
    private MSR msr = null;
    private SmartCardRW smartCardRW = null;
    private LocalSmartCardRW localSmartCardRW;
    private CashDrawer cashDrawer = null;

    private int mPortType;
    private String mAddress;

    public BixolonPrinter(Context context) {
        this.context = context;

        posPrinter = new POSPrinter(this.context);

        msr = new MSR();
        smartCardRW = new SmartCardRW();
        localSmartCardRW = new LocalSmartCardRW();
        cashDrawer = new CashDrawer();

        bxlConfigLoader = new BXLConfigLoader(this.context);
        try {
            bxlConfigLoader.openFile();

        } catch (Exception e) {

            bxlConfigLoader.newFile();
        }
    }

    public boolean printerOpen(int portType, String logicalName, String address, boolean isAsyncMode) {
        if (setTargetDevice(portType, logicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, address)) {
            int retry = 1;
            if (portType == BXLConfigLoader.DEVICE_BUS_BLUETOOTH_LE) {
                retry = 5;
            }

            for (int i = 0; i < retry; i++) {
                try {
                    posPrinter.open(logicalName);
                    posPrinter.claim(5000 * 2);
                    posPrinter.setDeviceEnabled(true);
                    posPrinter.setAsyncMode(isAsyncMode);

                    mPortType = portType;
                    mAddress = address;
                    return true;
                } catch (JposException e) {
                    e.printStackTrace();
                    try {
                        posPrinter.close();
                    } catch (JposException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    public boolean printerClose() {
        try {
            if (posPrinter.getClaimed()) {
                posPrinter.setDeviceEnabled(false);
                posPrinter.release();
                posPrinter.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean setTargetDevice(int portType, String logicalName, int deviceCategory, String address) {
        try {
            for (Object entry : bxlConfigLoader.getEntries()) {
                JposEntry jposEntry = (JposEntry) entry;
                if (jposEntry.getLogicalName().equals(logicalName)) {
                    bxlConfigLoader.removeEntry(logicalName);
                    break;
                }
            }

            bxlConfigLoader.addEntry(logicalName, deviceCategory, getProductName(logicalName), portType, address);

            bxlConfigLoader.saveFile();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
        return true;
    }

    private String getProductName(String name) {
        String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200II;

        if ((name.equals("SPP-R200III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200III;
        } else if ((name.equals("SPP-R210"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R210;
        } else if ((name.equals("SPP-R215"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R215;
        } else if ((name.equals("SPP-R220"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R220;
        } else if ((name.equals("SPP-C200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_C200;
        } else if ((name.equals("SPP-R300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R300;
        } else if ((name.equals("SPP-R310"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;
        } else if ((name.equals("SPP-R318"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R318;
        } else if ((name.equals("SPP-C300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_C300;
        } else if ((name.equals("SPP-R400"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R400;
        } else if ((name.equals("SPP-R410"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R410;
        } else if ((name.equals("SPP-R418"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R418;
        } else if ((name.equals("SPP-100II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_100II;
        } else if ((name.equals("SRP-350IIOBE"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350IIOBE;
        } else if ((name.equals("SRP-350III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350III;
        } else if ((name.equals("SRP-352III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352III;
        } else if ((name.equals("SRP-350V"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350V;
        } else if ((name.equals("SRP-352V"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352V;
        } else if ((name.equals("SRP-350plusIII"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSIII;
        } else if ((name.equals("SRP-352plusIII"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSIII;
        } else if ((name.equals("SRP-350plusV"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSV;
        } else if ((name.equals("SRP-352plusV"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSV;
        } else if ((name.equals("SRP-380"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_380;
        } else if ((name.equals("SRP-382"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_382;
        } else if ((name.equals("SRP-383"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_383;
        } else if ((name.equals("SRP-380II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_380II;
        } else if ((name.equals("SRP-382II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_382II;
        } else if ((name.equals("SRP-340II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_340II;
        } else if ((name.equals("SRP-342II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_342II;
        } else if ((name.equals("SRP-Q200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q200;
        } else if ((name.equals("SRP-Q300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q300;
        } else if ((name.equals("SRP-Q302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q302;
        } else if ((name.equals("SRP-QE300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE300;
        } else if ((name.equals("SRP-QE302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE302;
        } else if ((name.equals("SRP-E300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_E300;
        } else if ((name.equals("SRP-E302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_E302;
        } else if ((name.equals("SRP-B300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_B300;
        } else if ((name.equals("SRP-330II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_330II;
        } else if ((name.equals("SRP-332II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_332II;
        } else if ((name.equals("SRP-330III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_330III;
        } else if ((name.equals("SRP-332III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_332III;
        } else if ((name.equals("SRP-S200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S200;
        } else if ((name.equals("SRP-S300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S300;
        } else if ((name.equals("SRP-S320"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S320;
        } else if ((name.equals("SRP-S3000"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S3000;
        } else if ((name.equals("SRP-F310"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310;
        } else if ((name.equals("SRP-F312"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312;
        } else if ((name.equals("SRP-F310II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310II;
        } else if ((name.equals("SRP-F312II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312II;
        } else if ((name.equals("SRP-F313II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F313II;
        } else if ((name.equals("SRP-275III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_275III;
        } else if ((name.equals("BK3-2"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_BK3_2;
        } else if ((name.equals("BK3-3"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_BK3_3;
        } else if ((name.equals("SMB6350"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SMB6350;
        } else if ((name.equals("SLP X-Series"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SLP_X_SERIES;
        } else if ((name.equals("SLP-DX420"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SLP_DX420;
        } else if ((name.equals("SPP-L410II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_L410II;
        } else if ((name.equals("XM7-40"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_XM7_40;
        } else if ((name.equals("MSR"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_MSR;
        } else if ((name.equals("CashDrawer"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_CASH_DRAWER;
        } else if ((name.equals("LocalSmartCardRW"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_LOCAL_SMART_CARD_RW;
        } else if ((name.equals("SmartCardRW"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SMART_CARD_RW;
        }

        return productName;
    }

    public boolean printText(String data, int alignment, int attribute, int textSize) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            String strOption = EscapeSequence.getString(0);

            if ((alignment & ALIGNMENT_LEFT) == ALIGNMENT_LEFT) {
                strOption += EscapeSequence.getString(4);
            }

            if ((alignment & ALIGNMENT_CENTER) == ALIGNMENT_CENTER) {
                strOption += EscapeSequence.getString(5);
            }

            if ((alignment & ALIGNMENT_RIGHT) == ALIGNMENT_RIGHT) {
                strOption += EscapeSequence.getString(6);
            }

            if ((attribute & ATTRIBUTE_FONT_A) == ATTRIBUTE_FONT_A) {
                strOption += EscapeSequence.getString(1);
            }

            if ((attribute & ATTRIBUTE_FONT_B) == ATTRIBUTE_FONT_B) {
                strOption += EscapeSequence.getString(2);
            }

            if ((attribute & ATTRIBUTE_FONT_C) == ATTRIBUTE_FONT_C) {
                strOption += EscapeSequence.getString(3);
            }

            if ((attribute & ATTRIBUTE_FONT_D) == ATTRIBUTE_FONT_D) {
                strOption += EscapeSequence.getString(33);
            }

            if ((attribute & ATTRIBUTE_BOLD) == ATTRIBUTE_BOLD) {
                strOption += EscapeSequence.getString(7);
            }

            if ((attribute & ATTRIBUTE_UNDERLINE) == ATTRIBUTE_UNDERLINE) {
                strOption += EscapeSequence.getString(9);
            }

            if ((attribute & ATTRIBUTE_REVERSE) == ATTRIBUTE_REVERSE) {
                strOption += EscapeSequence.getString(11);
            }

            switch (textSize) {
                case 1:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(25);
                    break;
                case 2:
                    strOption += EscapeSequence.getString(18);
                    strOption += EscapeSequence.getString(26);
                    break;
                case 3:
                    strOption += EscapeSequence.getString(19);
                    strOption += EscapeSequence.getString(27);
                    break;
                case 4:
                    strOption += EscapeSequence.getString(20);
                    strOption += EscapeSequence.getString(28);
                    break;
                case 5:
                    strOption += EscapeSequence.getString(21);
                    strOption += EscapeSequence.getString(29);
                    break;
                case 6:
                    strOption += EscapeSequence.getString(22);
                    strOption += EscapeSequence.getString(30);
                    break;
                case 7:
                    strOption += EscapeSequence.getString(23);
                    strOption += EscapeSequence.getString(31);
                    break;
                case 8:
                    strOption += EscapeSequence.getString(24);
                    strOption += EscapeSequence.getString(32);
                    break;
                default:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(25);
                    break;
            }

            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + data);
        } catch (JposException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }

    public boolean printImage(String path, int width, int alignment, int brightness, int dither, int compress) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BM_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BM_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BM_RIGHT;
            }

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            buffer.put((byte) brightness); // brightness
            buffer.put((byte) compress); // compress
            buffer.put((byte) dither); // dither

            posPrinter.printBitmap(buffer.getInt(0), path, width, alignment);
        } catch (JposException e) {
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }

    public boolean printImage(Bitmap bitmap, int width, int alignment, int brightness, int dither, int compress) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BM_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BM_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BM_RIGHT;
            }

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            buffer.put((byte) brightness); // brightness
            buffer.put((byte) compress); // compress
            buffer.put((byte) dither); // dither

            posPrinter.printBitmap(buffer.getInt(0), bitmap, width, alignment);
        } catch (JposException e) {
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }

    public int getCoundPDFPages(Uri uri) {
        int pages = 0;

        try {
            pages = posPrinter.getCountPDFPages(uri);
        } catch (JposException e) {
            e.printStackTrace();
            return 0;
        }

        return pages;

    }

    public boolean printPdf(Uri uri, int width, int alignment, int startPage, int endPage, int brightness, int dither, int compress) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_PDF_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_PDF_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_PDF_RIGHT;
            }

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            buffer.put((byte) brightness); // brightness
            buffer.put((byte) compress); // compress
            buffer.put((byte) dither); // dither

            posPrinter.printPDFFile(buffer.getInt(0), uri, width, alignment, startPage, endPage);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }


    public boolean printSvg(String path, int width, int alignment, int brightness, int Rotate) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BM_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BM_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BM_RIGHT;
            }

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            buffer.put((byte) brightness); // brightness
            buffer.put((byte) 0x01); // compress
            buffer.put((byte) 0x00); // Reserve

            posPrinter.printSvg(buffer.getInt(0), path, width, alignment, Rotate);
        } catch (JposException e) {
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }

    public boolean printBarcode(String data, int symbology, int width, int height, int alignment, int hri) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BC_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BC_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BC_RIGHT;
            }

            posPrinter.printBarCode(POSPrinterConst.PTR_S_RECEIPT, data, symbology, height, width, alignment, hri);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    public boolean directIO(int command, byte[] data) {
        boolean ret = true;
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.directIO(command, null, data);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    public int getPrinterMaxWidth() {
        int width = 0;
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return width;
            }

            width = posPrinter.getRecLineWidth();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return width;
    }

    public boolean startPageMode(int xPos, int yPos, int width, int height, int direction) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            // "x,y,w,h"
            String area = xPos + "," + yPos + "," + width + "," + height;
            posPrinter.setPageModePrintArea(area);


            setPageModeDirection(direction);
            posPrinter.pageModePrint(POSPrinterConst.PTR_PM_PAGE_MODE);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean setPageModeDirection(int direction) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            // LEFT_TO_RIGHT = 1;
            // BOTTOM_TO_TOP = 2;
            // RIGHT_TO_LEFT = 3;
            // TOP_TO_BOTTOM = 4;
            posPrinter.setPageModePrintDirection(direction);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean setPageModePosition(int horizontal, int vertical) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.setPageModeHorizontalPosition(horizontal);
            posPrinter.setPageModeVerticalPosition(vertical);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean endPageMode(boolean isLabelMode) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.pageModePrint(POSPrinterConst.PTR_PM_NORMAL);
            if (isLabelMode) {
                formFeed();
            }
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean printBox(int startX, int startY, int stopX, int stopY, int thickness) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.printBox(startX, startY, stopX, stopY, thickness);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean printLine(int startX, int startY, int stopX, int stopY, int thickness) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.printLine(startX, startY, stopX, stopY, thickness);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean cutPaper() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            //String cutPaper = EscapeSequence.ESCAPE_CHARACTERS + String.format("%dfP", 100);  // Feed Full Cut
            String cutPaper = EscapeSequence.ESCAPE_CHARACTERS + String.format("%dfP", 90); // Feed Partial Cut
            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, cutPaper);    // Execute Feed cut

            // posPrinter.cutPaper(90);    // Normal Partial Cut
            //posPrinter.cutPaper(100);   // Normal Full Cut
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean ejectPaper(int mode) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.ejectPaper(mode);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public byte getPresenterStatus() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return -1;
            }

            return posPrinter.getPresenterStatus();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public boolean formFeed() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            // Form feed
            posPrinter.markFeed(0);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean beginTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_TRANSACTION);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean endTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_NORMAL);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean defineNvImage(String path, int code, int width, int brightness) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            buffer.put((byte) brightness); // brightness
            buffer.put((byte) 0x00); // Reserve
            buffer.put((byte) 0x00); // Reserve

            posPrinter.setBitmap(code, buffer.getInt(0), path, width, -1);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean printNVImage(int code) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            String nvCode = EscapeSequence.ESCAPE_CHARACTERS + String.format("%dB", code);

            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, nvCode);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean setCharacterSet(int cs) {
        try {
            posPrinter.setCharacterSet(cs);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public int getCharacterSet() {
        int cs = -1;
        try {
            cs = posPrinter.getCharacterSet();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return cs;
    }

    public String posPrinterCheckHealth() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return null;
            }

            posPrinter.checkHealth(JposConst.JPOS_CH_INTERNAL);
            posPrinter.checkHealth(JposConst.JPOS_CH_EXTERNAL);
            return posPrinter.getCheckHealthText();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getPosPrinterInfo() {
        String info;
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return null;
            }

            info = "deviceServiceDescription: " + posPrinter.getDeviceServiceDescription()
                    + "\ndeviceServiceVersion: " + posPrinter.getDeviceServiceVersion()
                    + "\nphysicalDeviceDescription: " + posPrinter.getPhysicalDeviceDescription()
                    + "\nphysicalDeviceName: " + posPrinter.getPhysicalDeviceName()
                    + "\npowerState: " + getPowerStateString(posPrinter.getPowerState())
                    + "\ncapRecNearEndSensor: " + posPrinter.getCapRecNearEndSensor()
                    + "\nRecPapercut: " + posPrinter.getCapRecPapercut()
                    + "\ncapRecMarkFeed: " + getMarkFeedString(posPrinter.getCapRecMarkFeed())
                    + "\ncharacterSet: " + posPrinter.getCharacterSet()
                    + "\ncharacterSetList: " + posPrinter.getCharacterSetList()
                    + "\nfontTypefaceList: " + posPrinter.getFontTypefaceList()
                    + "\nrecLineChars: " + posPrinter.getRecLineChars()
                    + "\nrecLineCharsList: " + posPrinter.getRecLineCharsList()
                    + "\nrecLineSpacing: " + posPrinter.getRecLineSpacing()
                    + "\nrecLineWidth: " + posPrinter.getRecLineWidth();

            return info;
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateFirmware(String path) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.updateFirmware(path);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayString(String data) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.displayString(data);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayStringAtLine(String data, int line) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.displayStringAtLine(line, data);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayClearScreen() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.clearScreen();
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayStoreImage(String path, int width, int height, int imageNumber) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.storeImageFile(path, width, height, imageNumber);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayStoreImage(Bitmap bitmap, int width, int height, int imageNumber) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.storeImage(bitmap, width, height, imageNumber);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayImage(int imageNumber, int xPos, int yPos) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.displayImage(imageNumber, xPos, yPos);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean displayClearImage(boolean isAll, int imageNumber) {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.clearImage(isAll, imageNumber);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean msrOpen() {
        if (setTargetDevice(mPortType, "MSR", BXLConfigLoader.DEVICE_CATEGORY_MSR, mAddress)) {
            try {
                if (posPrinter.getClaimed() == false) {
                    return false;
                }

                msr.open("MSR");
                msr.claim(5000);
                msr.setDeviceEnabled(true);
            } catch (JposException e) {
                e.printStackTrace();
                try {
                    msr.close();
                } catch (JposException e1) {
                    e1.printStackTrace();
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean msrClose() {
        try {
            if (msr.getClaimed()) {
                msr.setDeviceEnabled(false);
                msr.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean setDataEventEnabled(boolean enable) {
        try {
            if (msr.getDeviceEnabled() == false) {
                return false;
            }

            msr.setDataEventEnabled(enable);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public String getTrackData(int trackNumber) {
        String track = "";

        try {
            if (!msr.getDeviceEnabled()) {
                return track;
            }

            if (trackNumber == 1) {
                track = new String(msr.getTrack1Data());
            }

            if (trackNumber == 2) {
                track = new String(msr.getTrack2Data());
            }

            if (trackNumber == 3) {
                track = new String(msr.getTrack3Data());
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return track;
    }

    public String msrCheckHealth() {
        try {
            if (!msr.getDeviceEnabled()) {
                return null;
            }

            msr.checkHealth(JposConst.JPOS_CH_INTERNAL);
            return msr.getCheckHealthText();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getMsrInfo() {
        String info;
        try {
            if (!msr.getDeviceEnabled()) {
                return null;
            }

            info = "deviceServiceDescription: " + msr.getDeviceServiceDescription()
                    + "\ndeviceServiceVersion: " + msr.getDeviceServiceVersion()
                    + "\nphysicalDeviceDescription: " + msr.getPhysicalDeviceDescription()
                    + "\nphysicalDeviceName: " + msr.getPhysicalDeviceName()
                    + "\npowerState: " + getPowerStateString(msr.getPowerState())
                    + "\ncapDataEncryption: " + getDataEncryptionString(msr.getCapDataEncryption())
                    + "\ndataEncryptionAlgorithm: " + getDataEncryptionString(msr.getDataEncryptionAlgorithm())
                    + "\ntracksToRead: " + getTrackToReadString(msr.getTracksToRead());

            return info;
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean smartCardRWOpen() {
        if (setTargetDevice(mPortType, "SmartCardRW", BXLConfigLoader.DEVICE_CATEGORY_SMART_CARD_RW, mAddress)) {
            try {
                if (posPrinter.getClaimed() == false) {
                    return false;
                }

                smartCardRW.open("SmartCardRW");
                smartCardRW.claim(5000);
                smartCardRW.setDeviceEnabled(true);
            } catch (JposException e) {
                e.printStackTrace();
                try {
                    smartCardRW.close();
                } catch (JposException e1) {
                    e1.printStackTrace();
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean smartCardRWClose() {
        try {
            if (smartCardRW.getClaimed()) {
                smartCardRW.setDeviceEnabled(false);
                smartCardRW.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean SCPowerUp(int timeout) {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return false;
            }

            smartCardRW.beginInsertion(timeout);
            smartCardRW.endInsertion();
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean SCPowerDown(int timeout) {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return false;
            }

            smartCardRW.beginRemoval(timeout);
            smartCardRW.endRemoval();
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean setSCSlot(int slot) {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return false;
            }

            smartCardRW.setSCSlot(slot);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean setSCMode(int mode) {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return false;
            }

            // 1 : ISO
            // 2 : EMV
            smartCardRW.setIsoEmvMode(mode);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean SCRead(String[] data, int[] count) {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return false;
            }

            smartCardRW.readData(SmartCardRWConst.SC_READ_DATA, count, data);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public String smartCardCheckHealth() {
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return null;
            }

            smartCardRW.checkHealth(JposConst.JPOS_CH_INTERNAL);
            return smartCardRW.getCheckHealthText();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getSmartCardInfo() {
        String info;
        try {
            if (!smartCardRW.getDeviceEnabled()) {
                return null;
            }

            info = "deviceServiceDescription: " + smartCardRW.getDeviceServiceDescription()
                    + "\ndeviceServiceVersion: " + smartCardRW.getDeviceServiceVersion()
                    + "\nphysicalDeviceDescription: " + smartCardRW.getPhysicalDeviceDescription()
                    + "\nphysicalDeviceName: " + smartCardRW.getPhysicalDeviceName()
                    + "\npowerState: " + smartCardRW.getPowerState()
                    + "\ninterfaceMode: " + smartCardRW.getInterfaceMode()
                    + "\nisoEmvMode: " + smartCardRW.getIsoEmvMode()
                    + "\ntransactionInProgress: " + smartCardRW.getTransactionInProgress()
                    + "\ntransmissionProtocol: " + smartCardRW.getTransmissionProtocol();

            return info;
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean localSmartCardRWOpen() {
        if (setTargetDevice(mPortType, "LocalSmartCardRW", BXLConfigLoader.DEVICE_CATEGORY_LOCAL_SMART_CARD_RW, mAddress)) {
            try {
                if (posPrinter.getClaimed() == false) {
                    return false;
                }

                localSmartCardRW.open("LocalSmartCardRW");
                localSmartCardRW.claim(5000);
                localSmartCardRW.setDeviceEnabled(true);
            } catch (JposException e) {
                e.printStackTrace();
                try {
                    localSmartCardRW.close();
                } catch (JposException e1) {
                    e1.printStackTrace();
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean localSmartCardRWClose() {
        try {
            if (localSmartCardRW.getClaimed()) {
                localSmartCardRW.setDeviceEnabled(false);
                localSmartCardRW.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean getLocalCardInfo(int timeout) {
        try {
            if (!localSmartCardRW.getClaimed()) {
                return false;
            }

            localSmartCardRW.getCardInfo(timeout);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public byte[] getLocalTrack2() {
        byte[] cardNumber = null;
        try {
            if (!localSmartCardRW.getClaimed()) {
                return null;
            }

            cardNumber = localSmartCardRW.getTrack2();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return cardNumber;
    }

    public byte[] getLocalCardNumber() {
        byte[] cardNumber = null;
        try {
            if (!localSmartCardRW.getClaimed()) {
                return null;
            }

            cardNumber = localSmartCardRW.getCardNumber();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return cardNumber;
    }

    public byte[] getLocalCardDueDate() {
        byte[] cardDueDate = null;
        try {
            if (!localSmartCardRW.getClaimed()) {
                return null;
            }

            cardDueDate = localSmartCardRW.getCardDueDate();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return cardDueDate;
    }

    public boolean cashDrawerOpen() {
        if (setTargetDevice(mPortType, "CashDrawer", BXLConfigLoader.DEVICE_CATEGORY_CASH_DRAWER, mAddress)) {
            try {
                cashDrawer.open("CashDrawer");
                cashDrawer.claim(5000);
                cashDrawer.setDeviceEnabled(true);
            } catch (JposException e) {
                e.printStackTrace();
                try {
                    cashDrawer.close();
                } catch (JposException e1) {
                    e1.printStackTrace();
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean drawerOpen() {
        try {
            if (!cashDrawer.getDeviceEnabled()) {
                return false;
            }

            cashDrawer.openDrawer();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean cashDrawerClose() {
        try {
            if (cashDrawer.getClaimed()) {
                cashDrawer.setDeviceEnabled(false);
                cashDrawer.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    public String cashDrawerCheckHealth() {
        try {
            if (!cashDrawer.getDeviceEnabled()) {
                return null;
            }

            cashDrawer.checkHealth(JposConst.JPOS_CH_INTERNAL);
            return cashDrawer.getCheckHealthText();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getCashDrawerInfo() {
        String info;
        try {
            if (!cashDrawer.getDeviceEnabled()) {
                return null;
            }

            info = "deviceServiceDescription: " + cashDrawer.getDeviceServiceDescription()
                    + "\ndeviceServiceVersion: " + cashDrawer.getDeviceServiceVersion()
                    + "\nphysicalDeviceDescription: " + cashDrawer.getPhysicalDeviceDescription()
                    + "\nphysicalDeviceName: " + cashDrawer.getPhysicalDeviceName()
                    + "\npowerState: " + getPowerStateString(cashDrawer.getPowerState());

            return info;
        } catch (JposException e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] StringToHex(String strScr) {
        byte[] src = strScr.getBytes();
        byte[] dst = null;
        int nLength = src.length;

        dst = ConvertHexaToInteger(src, nLength);

        return dst;
    }

    private byte[] ConvertHexaToInteger(byte[] szHexa, int nSize) {
        ByteBuffer hex = ByteBuffer.allocate(nSize / 2);
        int ch1, ch2, bData;
        int j = 0;

        while (j < nSize) {
            ch1 = ConvertINT(szHexa[j]);
            bData = (byte) (ch1 << 4);
            j++;
            ch2 = ConvertINT(szHexa[j]);
            bData += (ch2 & 0x0f);
            j++;
            hex.put((byte) bData);
        }

        return hex.array();
    }

    private byte ConvertINT(int h) {
        int n = h < 0 ? h + 255 : h;
        return (byte) ((n > '9') ? 10 + (n - 'A') : (n - '0'));
    }

    private String getERMessage(int status) {
        switch (status) {
            case POSPrinterConst.JPOS_EPTR_COVER_OPEN:
                return "Cover open";

            case POSPrinterConst.JPOS_EPTR_REC_EMPTY:
                return "Paper empty";

            case POSPrinterConst.JPOS_EPTR_OFF_LINE:
                return "Printer Off-line";

            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
                return "Power off";

            default:
                return "Unknown";
        }
    }

    private String getSUEMessage(int status) {
        switch (status) {
            case JposConst.JPOS_SUE_POWER_ONLINE:
                return "StatusUpdate : Power on";

            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
                //printerClose();
                return "StatusUpdate : Power off";

            case POSPrinterConst.PTR_SUE_COVER_OPEN:
                return "StatusUpdate : Cover Open";

            case POSPrinterConst.PTR_SUE_COVER_OK:
                return "StatusUpdate : Cover OK";

            case POSPrinterConst.PTR_SUE_BAT_LOW:
                return "StatusUpdate : Battery-Low";

            case POSPrinterConst.PTR_SUE_BAT_OK:
                return "StatusUpdate : Battery-OK";

            case POSPrinterConst.PTR_SUE_REC_EMPTY:
                return "StatusUpdate : Receipt Paper Empty";

            case POSPrinterConst.PTR_SUE_REC_NEAREMPTY:
                return "StatusUpdate : Receipt Paper Near Empty";

            case POSPrinterConst.PTR_SUE_REC_PAPEROK:
                return "StatusUpdate : Receipt Paper OK";

            case POSPrinterConst.PTR_SUE_IDLE:
                return "StatusUpdate : Printer Idle";

            case POSPrinterConst.PTR_SUE_OFF_LINE:
                return "StatusUpdate : Printer off line";

            case POSPrinterConst.PTR_SUE_ON_LINE:
                return "StatusUpdate : Printer on line";

            default:
                return "StatusUpdate : Unknown";
        }
    }

    private String getBatterStatusString(int status) {
        switch (status) {
            case 0x30:
                return "BatterStatus : Full";

            case 0x31:
                return "BatterStatus : High";

            case 0x32:
                return "BatterStatus : Middle";

            case 0x33:
                return "BatterStatus : Low";

            default:
                return "BatterStatus : Unknwon";
        }
    }

    private String getPowerStateString(int powerState) {
        switch (powerState) {
            case JposConst.JPOS_PS_OFF_OFFLINE:
                return "OFFLINE";

            case JposConst.JPOS_PS_ONLINE:
                return "ONLINE";

            default:
                return "Unknown";
        }
    }

    private String getMarkFeedString(int markFeed) {
        switch (markFeed) {
            case POSPrinterConst.PTR_MF_TO_TAKEUP:
                return "TAKEUP";

            case POSPrinterConst.PTR_MF_TO_CUTTER:
                return "CUTTER";

            case POSPrinterConst.PTR_MF_TO_CURRENT_TOF:
                return "CURRENT TOF";

            case POSPrinterConst.PTR_MF_TO_NEXT_TOF:
                return "NEXT TOF";

            default:
                return "Not support";
        }
    }

    private String getDataEncryptionString(int dataEncryption) {
        switch (dataEncryption) {
            case MSRConst.MSR_DE_NONE:
                return "Data encryption is not enabled";

            case MSRConst.MSR_DE_3DEA_DUKPT:
                return "Triple DEA encryption";

            default:
                return "Additional encryption algorithms supported";
        }
    }

    private String getTrackToReadString(int tracksToRead) {
        switch (tracksToRead) {
            case MSRConst.MSR_TR_1:
                return "Track 1";

            case MSRConst.MSR_TR_2:
                return "Track 2";

            case MSRConst.MSR_TR_3:
                return "Track 3";

            case MSRConst.MSR_TR_1_2:
                return "Track 1 and 2";

            case MSRConst.MSR_TR_2_3:
                return "Track 2 and 3";

            case MSRConst.MSR_TR_1_2_3:
                return "Track 1, 2 and 3";

            default:
                return "MSR does not support reading track data";
        }
    }
}
