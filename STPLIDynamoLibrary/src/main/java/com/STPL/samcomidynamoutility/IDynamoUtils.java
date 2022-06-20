package com.STPL.samcomidynamoutility;


import com.magtek.mobile.android.mtlib.MTSCRA;

public class IDynamoUtils {

    public static String getCardInfo(MTSCRA m_scra) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Tracks.Masked=%s \n", m_scra.getMaskedTracks()));
        stringBuilder.append(String.format("Track1.Encrypted=%s \n", m_scra.getTrack1()));
        stringBuilder.append(String.format("Track2.Encrypted=%s \n", m_scra.getTrack2()));
        stringBuilder.append(String.format("Track3.Encrypted=%s \n", m_scra.getTrack3()));
        stringBuilder.append(String.format("Track1.Masked=%s \n", m_scra.getTrack1Masked()));
        stringBuilder.append(String.format("Track2.Masked=%s \n", m_scra.getTrack2Masked()));
        stringBuilder.append(String.format("Track3.Masked=%s \n", m_scra.getTrack3Masked()));
        stringBuilder.append(String.format("MagnePrint.Encrypted=%s \n", m_scra.getMagnePrint()));
        stringBuilder.append(String.format("MagnePrint.Status=%s \n", m_scra.getMagnePrintStatus()));
        stringBuilder.append(String.format("Device.Serial=%s \n", m_scra.getDeviceSerial()));
        stringBuilder.append(String.format("Session.ID=%s \n", m_scra.getSessionID()));
        stringBuilder.append(String.format("KSN=%s \n", m_scra.getKSN()));

        //stringBuilder.append(formatStringIfNotEmpty("Device.Name=%s \n", m_scra.getDeviceName()));
        //stringBuilder.append(String.format("Swipe.Count=%d \n", m_scra.getSwipeCount()));

        stringBuilder.append(formatStringIfNotEmpty("Cap.MagnePrint=%s \n", m_scra.getCapMagnePrint()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagnePrintEncryption=%s \n", m_scra.getCapMagnePrintEncryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagneSafe20Encryption=%s \n", m_scra.getCapMagneSafe20Encryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagStripeEncryption=%s \n", m_scra.getCapMagStripeEncryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MSR=%s \n", m_scra.getCapMSR()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.Tracks=%s \n", m_scra.getCapTracks()));

        stringBuilder.append(String.format("Card.Data.CRC=%d \n", m_scra.getCardDataCRC()));
        stringBuilder.append(String.format("Card.Exp.Date=%s \n", m_scra.getCardExpDate()));
        stringBuilder.append(String.format("Card.IIN=%s \n", m_scra.getCardIIN()));
        stringBuilder.append(String.format("Card.Last4=%s \n", m_scra.getCardLast4()));
        stringBuilder.append(String.format("Card.Name=%s \n", m_scra.getCardName()));
        stringBuilder.append(String.format("Card.PAN=%s \n", m_scra.getCardPAN()));
        stringBuilder.append(String.format("Card.PAN.Length=%d \n", m_scra.getCardPANLength()));
        stringBuilder.append(String.format("Card.Service.Code=%s \n", m_scra.getCardServiceCode()));
        stringBuilder.append(String.format("Card.Status=%s \n", m_scra.getCardStatus()));

        stringBuilder.append(formatStringIfNotEmpty("HashCode=%s \n", m_scra.getHashCode()));
        stringBuilder.append(formatStringIfNotValueZero("Data.Field.Count=%s \n", m_scra.getDataFieldCount()));

        stringBuilder.append(String.format("Encryption.Status=%s \n", m_scra.getEncryptionStatus()));

        //stringBuilder.append(formatStringIfNotEmpty("Firmware=%s \n", m_scra.getFirmware()));

        stringBuilder.append(formatStringIfNotEmpty("MagTek.Device.Serial=%s \n", m_scra.getMagTekDeviceSerial()));

        stringBuilder.append(formatStringIfNotEmpty("Response.Type=%s \n", m_scra.getResponseType()));
        stringBuilder.append(formatStringIfNotEmpty("TLV.Version=%s \n", m_scra.getTLVVersion()));

        stringBuilder.append(String.format("Track.Decode.Status=%s \n", m_scra.getTrackDecodeStatus()));

        String tkStatus = m_scra.getTrackDecodeStatus();

        String tk1Status = "01";
        String tk2Status = "01";
        String tk3Status = "01";

        if (tkStatus.length() >= 6) {
            tk1Status = tkStatus.substring(0, 2);
            tk2Status = tkStatus.substring(2, 4);
            tk3Status = tkStatus.substring(4, 6);

            stringBuilder.append(String.format("Track1.Status=%s \n", tk1Status));
            stringBuilder.append(String.format("Track2.Status=%s \n", tk2Status));
            stringBuilder.append(String.format("Track3.Status=%s \n", tk3Status));
        }
        stringBuilder.append(String.format("SDK.Version=%s \n", m_scra.getSDKVersion()));
        stringBuilder.append(String.format("Battery.Level=%d \n", m_scra.getBatteryLevel()));
        return stringBuilder.toString();
    }
    public static String formatStringIfNotEmpty(String format, String data) {
        String result = "";

        if (!data.isEmpty()) {
            result = String.format(format, data);
        }

        return result;
    }



    public static byte[] buildAcquirerResponseFormat1(byte[] macKSN, byte[] macEncryptionType, byte[] deviceSN, boolean approved) {
        byte[] response = null;

        int lenMACKSN = 0;
        int lenMACEncryptionType = 0;
        int lenSN = 0;

        if (macKSN != null) {
            lenMACKSN = macKSN.length;
        }

        if (macEncryptionType != null) {
            lenMACEncryptionType = macEncryptionType.length;
        }

        if (deviceSN != null) {
            lenSN = deviceSN.length;
        }

        byte[] macKSNTag = new byte[]{(byte) 0xDF, (byte) 0xDF, 0x54, (byte) lenMACKSN};
        byte[] macEncryptionTypeTag = new byte[]{(byte) 0xDF, (byte) 0xDF, 0x55, (byte) lenMACEncryptionType};
        byte[] snTag = new byte[]{(byte) 0xDF, (byte) 0xDF, 0x25, (byte) lenSN};
        byte[] container = new byte[]{(byte) 0xFA, 0x06, 0x70, 0x04};
        byte[] approvedARC = new byte[]{(byte) 0x8A, 0x02, 0x30, 0x30};
        byte[] declinedARC = new byte[]{(byte) 0x8A, 0x02, 0x30, 0x35};

        int lenTLV = 4 + macKSNTag.length + lenMACKSN + macEncryptionTypeTag.length + lenMACEncryptionType + snTag.length + lenSN + container.length + approvedARC.length;

        int lenPadding = 0;

        if ((lenTLV % 8) > 0) {
            lenPadding = (8 - lenTLV % 8);
        }

        int lenData = lenTLV + lenPadding + 4;

        response = new byte[lenData];

        int i = 0;
        response[i++] = (byte) (((lenData - 2) >> 8) & 0xFF);
        response[i++] = (byte) ((lenData - 2) & 0xFF);
        response[i++] = (byte) 0xF9;
        response[i++] = (byte) (lenTLV - 4);
        System.arraycopy(macKSNTag, 0, response, i, macKSNTag.length);
        i += macKSNTag.length;
        System.arraycopy(macKSN, 0, response, i, macKSN.length);
        i += macKSN.length;
        System.arraycopy(macEncryptionTypeTag, 0, response, i, macEncryptionTypeTag.length);
        i += macEncryptionTypeTag.length;
        System.arraycopy(macEncryptionType, 0, response, i, macEncryptionType.length);
        i += macEncryptionType.length;
        System.arraycopy(snTag, 0, response, i, snTag.length);
        i += snTag.length;
        System.arraycopy(deviceSN, 0, response, i, deviceSN.length);
        i += deviceSN.length;
        System.arraycopy(container, 0, response, i, container.length);
        i += container.length;

        if (approved) {
            System.arraycopy(approvedARC, 0, response, i, approvedARC.length);
        } else {
            System.arraycopy(declinedARC, 0, response, i, declinedARC.length);
        }

        return response;
    }
    public static byte[] buildAcquirerResponseFormat0(byte[] deviceSN, boolean approved) {
        byte[] response = null;

        int lenSN = 0;

        if (deviceSN != null)
            lenSN = deviceSN.length;

        byte[] snTag = new byte[]{(byte) 0xDF, (byte) 0xDF, 0x25, (byte) lenSN};
        byte[] container = new byte[]{(byte) 0xFA, 0x06, 0x70, 0x04};
        byte[] approvedARC = new byte[]{(byte) 0x8A, 0x02, 0x30, 0x30};
        byte[] declinedARC = new byte[]{(byte) 0x8A, 0x02, 0x30, 0x35};

        int len = 4 + snTag.length + lenSN + container.length + approvedARC.length;

        response = new byte[len];

        int i = 0;
        len -= 2;
        response[i++] = (byte) ((len >> 8) & 0xFF);
        response[i++] = (byte) (len & 0xFF);
        len -= 2;
        response[i++] = (byte) 0xF9;
        response[i++] = (byte) len;
        System.arraycopy(snTag, 0, response, i, snTag.length);
        i += snTag.length;
        System.arraycopy(deviceSN, 0, response, i, deviceSN.length);
        i += deviceSN.length;
        System.arraycopy(container, 0, response, i, container.length);
        i += container.length;
        if (approved) {
            System.arraycopy(approvedARC, 0, response, i, approvedARC.length);
        } else {
            System.arraycopy(declinedARC, 0, response, i, declinedARC.length);
        }
        return response;
    }

    public static String formatStringIfNotValueZero(String format, int data) {
        String result = "";

        if (data != 0) {
            result = String.format(format, data);
        }

        return result;
    }

      public static String getCardType(String type) {
        switch (type) {
            case "06":
                return "TAP";
            case "07":
                return "SWIPE";
            case "05":
                return "CHIP";
        }
        return "null";
    }
}
