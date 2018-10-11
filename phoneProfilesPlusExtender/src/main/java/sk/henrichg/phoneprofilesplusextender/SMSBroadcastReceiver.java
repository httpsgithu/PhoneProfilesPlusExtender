package sk.henrichg.phoneprofilesplusextender;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Calendar;

public class SMSBroadcastReceiver extends BroadcastReceiver {

    //private static ContentObserver smsObserver;
    //private static ContentObserver mmsObserver;
    //private static int mmsCount;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("##### SMSBroadcastReceiver.onReceive", "xxx");

        boolean smsMmsReceived = false;

        String origin = "";
        //String body = "";

        //String smsAction = "android.provider.Telephony.SMS_RECEIVED";
        //String mmsAction = "android.provider.Telephony.WAP_PUSH_RECEIVED";
        //if (android.os.Build.VERSION.SDK_INT >= 19) {
            String smsAction = Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
            String mmsAction = Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
        //}

        if ((intent != null) && (intent.getAction() != null) && intent.getAction().equals(smsAction))
        {
            Log.e("SMSBroadcastReceiver.onReceive","SMS received");

            smsMmsReceived = true;

            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object[] pdus = (Object[]) extras.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                        origin = msg.getOriginatingAddress();
                        //body = msg.getMessageBody();
                    }
                }
            }
        }
        /*else
        if(intent.getAction().equals("android.provider.Telephony.SMS_SENT"))
        {
            PPApplication.logE("SMSBroadcastReceiver.onReceive","sent");
        }*/
        else
        if ((intent != null) && (intent.getAction() != null) && intent.getAction().equals(mmsAction)) {
            String type = intent.getType();

            Log.e("SMSBroadcastReceiver.onReceive", "MMS received");
            Log.e("SMSBroadcastReceiver.onReceive", "type="+type);

            if ((type != null) && type.equals("application/vnd.wap.mms-message")) {

                smsMmsReceived = true;

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    byte[] buffer = bundle.getByteArray("data");
                    if (buffer != null) {
                        String incomingNumber = new String(buffer);
                        int idx = incomingNumber.indexOf("/TYPE");

                        if (idx > 0 && (idx - 15) > 0) {
                            int newIdx = idx - 15;
                            incomingNumber = incomingNumber.substring(newIdx, idx);
                            idx = incomingNumber.indexOf("+");
                            if (idx > 0) {
                                origin = incomingNumber.substring(idx);
                            }
                        }
                    }
                /*int transactionId = bundle.getInt("transactionId");
                int pduType = bundle.getInt("pduType");
                byte[] buffer2 = bundle.getByteArray("header");      
                String header = new String(buffer2);*/
                }
            }
        }

        Log.e("@@@ SMSBroadcastReceiver.onReceive","smsMmsReceived="+smsMmsReceived);

        if (smsMmsReceived)
        {
            Log.e("SMSBroadcastReceiver.onReceive","from="+origin);

            Calendar now = Calendar.getInstance();
            int gmtOffset = 0; //TimeZone.getDefault().getRawOffset();
            long time = now.getTimeInMillis() + gmtOffset;

            //TODO send to PPP origin and time
        }
    }

/*
    private static final String CONTENT_SMS = "content://sms";
    // Constant from Android SDK
    private static final int MESSAGE_TYPE_SENT = 2;


    // Register an observer for listening outgoing sms events.
    // @author khoanguyen
    static public void registerSMSContentObserver(Context context)
    {
        if (smsObserver != null)
            return;

        final Context _context = context;

        smsObserver = new ContentObserver(null)
        {
            public void onChange(boolean selfChange)
            {
                PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","xxx");

                // read outgoing sms from db
                Cursor cursor = _context.getContentResolver().query(Uri.parse(CONTENT_SMS), null, null, null, null);
                if (cursor.moveToNext())
                {
                    String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
                    int type = cursor.getInt(cursor.getColumnIndex("type"));
                    // Only processing outgoing sms event & only when it
                    // is sent successfully (available in SENT box).
                    if (protocol != null || type != MESSAGE_TYPE_SENT)
                    {
                        PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","no SMS in SENT box");
                        return;
                    }
                    int dateColumn = cursor.getColumnIndex("date");
                    //int bodyColumn = cursor.getColumnIndex("body");
                    int addressColumn = cursor.getColumnIndex("address");

                    String to = cursor.getString(addressColumn);
                    Date date = new Date(cursor.getLong(dateColumn));
                    //String message = cursor.getString(bodyColumn);

                    PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","sms sent");
                    PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","to="+to);
                    PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","date="+date);
                    //PPApplication.logE("SMSBroadcastReceiver.smsObserver.onChange","message="+message);

                    SharedPreferences preferences = _context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);
                    Editor editor = preferences.edit();
                    editor.putInt(PPApplication.PREF_EVENT_SMS_EVENT_TYPE, EventPreferencesSMS.SMS_EVENT_OUTGOING);
                    editor.putString(PPApplication.PREF_EVENT_SMS_PHONE_NUMBER, to);
                    int gmtOffset = TimeZone.getDefault().getRawOffset();
                    long time = date.getTime() + gmtOffset;
                    editor.putLong(PPApplication.PREF_EVENT_SMS_DATE, time);
                    editor.commit();
                }
                cursor.close();

                startService(_context);
            }
        };

        context.getContentResolver().registerContentObserver(Uri.parse(CONTENT_SMS), true, smsObserver);
    }

    public static void unregisterSMSContentObserver(Context context)
    {
        if (smsObserver != null)
            context.getContentResolver().unregisterContentObserver(smsObserver);
    }

    // not working with with Hangouts :-/
    static public void registerMMSContentObserver(Context context)
    {
        if (mmsObserver != null)
            return;

        Uri uriMMS = Uri.parse("content://mms");
        Cursor mmsCur = context.getContentResolver().query(uriMMS, null, "msg_box = 4", null, "_id");
        if (mmsCur != null && mmsCur.getCount() > 0) {
           mmsCount = mmsCur.getCount();
        }

        final Context _context = context;

        mmsObserver = new ContentObserver(null)
        {
            public void onChange(boolean selfChange)
            {
                PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","xxx");

                // read outgoing mms from db
                Uri uriMMS = Uri.parse("content://mms/");
                Cursor mmsCur = _context.getContentResolver().query(uriMMS, null, "msg_box = 4 or msg_box = 1", null,"_id");

                int currMMSCount = 0;
                if (mmsCur != null && mmsCur.getCount() > 0) {
                   currMMSCount = mmsCur.getCount();
                }

                PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","mmsCount="+mmsCount);
                PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","currMMSCount="+currMMSCount);
                
                if (currMMSCount > mmsCount)
                {
                    if (mmsCur.moveToLast())
                    {
                        // 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
                        int type = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("m_type")));

                        PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","type="+type);

                        if (type == 128) {
                           // Outgoing MMS

                            PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","mms sent");

                            int id = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("_id")));

                            // Get Address
                            Uri uriAddrPart = Uri.parse("content://mms/"+id+"/addr");
                            Cursor addrCur = _context.getContentResolver().query(uriAddrPart, null, "type=151", null, "_id");
                            if (addrCur != null)
                            {
                                if (addrCur.moveToLast())
                                {
                                    do
                                    {
                                        int addColIndx = addrCur.getColumnIndex("address");
                                        String to = addrCur.getString(addColIndx);

                                        PPApplication.logE("SMSBroadcastReceiver.mmsObserver.onChange","to="+to);

                                        SharedPreferences preferences = _context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);
                                        Editor editor = preferences.edit();
                                        editor.putInt(PPApplication.PREF_EVENT_SMS_EVENT_TYPE, EventPreferencesSMS.SMS_EVENT_OUTGOING);
                                        editor.putString(PPApplication.PREF_EVENT_SMS_PHONE_NUMBER, to);
                                        Calendar now = Calendar.getInstance();
                                        int gmtOffset = TimeZone.getDefault().getRawOffset();
                                        long time = now.getTimeInMillis() + gmtOffset;
                                        editor.putLong(PPApplication.PREF_EVENT_SMS_DATE, time);
                                        editor.commit();

                                        startService(_context);
                                    }
                                    while (addrCur.moveToPrevious());
                                }
                            }
                        }
                    }
                }
                
                mmsCount = currMMSCount;
                
            }
        };

        context.getContentResolver().registerContentObserver(Uri.parse("content://mms-sms"), true, mmsObserver);
    }

    public static void unregisterMMSContentObserver(Context context)
    {
        if (mmsObserver != null)
            context.getContentResolver().unregisterContentObserver(mmsObserver);
    }
*/
}
