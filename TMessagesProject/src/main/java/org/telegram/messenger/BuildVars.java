package org.telegram.messenger;

/**
 * Official Telegram Build Variables
 * Configured for Telegram_Anwer based on DrKLO/Telegram official specifications
 * Target Repository: anwer1230/Telegram_pro (from DrKLO/Telegram)
 */
public class BuildVars {
    public static boolean DEBUG_VERSION = false;
    public static boolean LOGS_ENABLED = false;
    public static boolean DEBUG_PRIVATE_VERSION = false;
    
    // Telegram Official MTProto API Credentials
    public static final int APP_ID = 22043994;
    public static final String APP_HASH = "56f64582b363d367280db96586b97801";
    
    // Application Metadata & Versioning
    public static final String BUILD_VERSION = "12.9.2";
    public static final String BUILD_VERSION_STRING = "12.9.2";
    public static final int BUILD_VERSION_ID = 4980;
    public static final int BUILD_VERSION_NUM = 4980;
    public static final String APP_NAME = "Telegram_Anwer";
    
    // Push Notifications & SMS Service
    public static String SMS_HASH = "";
    public static String PLAYSTORE_APP_URL = "";
    public static String GOOGLE_AUTH_CLIENT_ID = "";
    
    // Package & Security Information
    public static String APPLICATION_ID = "org.telegram.messenger.anwer";
    public static String RELEASE_KEY_ALIAS = "Telegram_Anwer";
    public static String RELEASE_KEY_PASSWORD = "772997043a**";
    public static String RELEASE_STORE_PASSWORD = "772997043a**";
    public static String NDK_VERSION = "27.2.12479018";
    public static int TARGET_SDK = 35;
    public static int MIN_SDK = 23;
}
