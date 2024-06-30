package com.bunny.entertainment.anify.utils;

public class Constants {
    public static final String PREFS_NAME = "com.bunny.entertainment.anify.prefs";
    public static final String PREFS_NSFW_SWITCH = "nsfw_switch";
    public static final String PREF_VERSION_CODE_KEY = "version_code";
    public static final int REQUEST_INSTALL_PACKAGES = 1001;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 1002;
    public static final String API_ERROR_MESSAGE = "API Error! Unable to fetch fact. Please try again later.";
    public static final String WAIFU_IT_API_KEY_ERROR = "You have not added the API key or the API key is invalid";
    public static final String WAIFU_IT_API_KEY = "waifu_im_api_key";
    public static final String PREF_CACHE_REMOVAL_INTERVAL = "cache_removal_interval";
    public static final String PREF_FACT_UPDATE_INTERVAL = "fact_update_interval";
    public static final String PREF_ANIME_FACT_UPDATE_INTERVAL = "anime_fact_update_interval";
    public static final String PREF_ANIME_IMAGE_UPDATE_INTERVAL = "anime_image_update_interval";
    public static final String PREF_FACT_LAST_UPDATE_TIME = "fact_last_update_interval";
    public static final String PREF_ANIME_FACT_LAST_UPDATE_TIME = "anime_fact_last_update_interval";
    public static final String PREF_ANIME_IMAGE_LAST_UPDATE_TIME = "anime_image_last_update_interval";
    public static final String PREF_LAST_FACT = "last_fact";
    public static final String PREF_LAST_ANIME_FACT = "last_anime_fact";
    public static final String PREF_LAST_ANIME_IMAGE_URL = "last_anime_image_url";
    public static final String ACTION_REFRESH = "com.bunny.entertainment.anify.widgets.ACTION_REFRESH";
    public static final String ACTION_RESET_ALARM = "com.bunny.entertainment.anify.widgets.ACTION_RESET_ALARM";
    public static final String ACTION_AUTO_UPDATE = "com.bunny.entertainment.anify.widgets.ACTION_AUTO_UPDATE";
    public static final String ACTION_UPDATE_FINISHED = "com.bunny.entertainment.anify.widgets.ACTION_UPDATE_FINISHED";
    public static final String ACTION_WAIFU_IM_API_KEY_UPDATED = "com.bunny.entertainment.anify.widgets.ACTION_API_KEY_UPDATED";
    public static final String PREF_LAST_WAIFU_PICS_CATEGORY = "last_waifu_pics_category";
    public static final String PREF_LAST_NSFW_WAIFU_PICS_CATEGORY = "last_nsfw_waifu_pics_category";
    public static final String PREF_LAST_NEKOBOT_CATEGORY = "last_nekobot_category";
    public static final String PREF_LAST_NSFW_NEKOBOT_CATEGORY = "last_nsfw_nekobot_category";
    public static final String PREF_LAST_WAIFU_IM_CATEGORY = "last_nsfw_waifu_im_category";
    public static final String PREF_LAST_NSFW_WAIFU_IM_CATEGORY = "last_waifu_im_category";
    public static final long[] CACHE_INTERVALS = {
            0L,                  // Off
            24 * 60 * 60 * 1000L,    // 1 day
            2 * 24 * 60 * 60 * 1000L,   // 2 days
            3 * 24 * 60 * 60 * 1000L,   // 3 days
            5 * 24 * 60 * 60 * 1000L,   // 5 days
            7 * 24 * 60 * 60 * 1000L,   // 7 days
            14 * 24 * 60 * 60 * 1000L,  // 14 days
            30 * 24 * 60 * 60 * 1000L   // 30 days
    };
    public static final long[] INTERVALS = {
            0L,                 // Off
            5 * 60 * 1000L,    // 5 minutes
            10 * 60 * 1000L,   // 10 minutes
            20 * 60 * 1000L,   // 20 minutes
            30 * 60 * 1000L,   // 30 minutes
            60 * 60 * 1000L,   // 1 hour
            2 * 60 * 60 * 1000L,   // 2 hours
            3 * 60 * 60 * 1000L,   // 3 hours
            4 * 60 * 60 * 1000L,   // 4 hours
            5 * 60 * 60 * 1000L,   // 5 hours
            6 * 60 * 60 * 1000L,   // 6 hours
            8 * 60 * 60 * 1000L,   // 8 hours
            10 * 60 * 60 * 1000L,  // 10 hours
            12 * 60 * 60 * 1000L   // 12 hours
    };
    public static final long DEFAULT_CACHE_REMOVAL_INTERVAL = 7 * 24 * 60 * 60 * 1000; // 7 days
    public static final long DEFAULT_INTERVAL = INTERVALS[5]; // 1 hour
    public static final String[] API_SOURCES = {"Waifu.pics", "NekoBot", "waifu.im"};
    public static final String PREF_API_SOURCE = "anime_image_api_source";
    public static final String API_WAIFU_PICS = "waifu_pics";
    public static final String API_WAIFU_PICS_NSFW = "waifu_pics_nsfw";
    public static final String API_NEKOBOT_NSFW = "nekobot_nsfw";
    public static final String API_WAIFU_IM_NSFW = "waifu_im_nsfw";
    public static final String API_NEKOBOT = "nekobot";
    public static final String API_WAIFU_IM = "waifu_im";
    public static String[] categorySfwWaifuPics = {"waifu", "neko", "shinobu", "megumin", "awoo", "nom", "happy", "wink", "cringe"};
    public static String[] categoryNsfwWaifuPics = {"waifu", "neko", "trap", "blowjob"};
    public static String[] categorySfwNekobot = {"neko", "kemonomimi", "kanna", "coffee", "food"};
    public static String[] categoryNsfwNekobot = {"hmidriff", "hentai", "holo", "hneko", "hkitsune", "thigh", "hthigh", "paizuri", "tentacle", "hboobs"};
    public static String[] categorySfwWaifuIm = {"waifu", "maid", "marin-kitagawa", "mori-calliope", "raiden-shogun", "oppai", "selfies", "uniform", "kamisato-ayaka"};
    public static String[] categoryNsfwWaifuIm = {"ero", "ass", "hentai", "milf", "oral", "paizuri", "ecchi"};

}
