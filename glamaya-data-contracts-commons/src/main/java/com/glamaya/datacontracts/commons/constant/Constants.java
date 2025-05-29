package com.glamaya.datacontracts.commons.constant;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

public class Constants {

    private Constants() {
        // Prevent instantiation
    }

    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int SIX = 6;
    public static final int SEVEN = 7;
    public static final int EIGHT = 8;
    public static final int NINE = 9;
    public static final int TEN = 10;
    public static final int TWENTY = 20;
    public static final int FIFTY = 50;
    public static final int NINETY_NINE = 99;
    public static final int HUNDRED = 100;

    public static final String COMMA = ",";
    public static final String PIPE_REGEX = "\\|";
    public static final String SPACE = " ";
    public static final String COLON = ":";
    public static final String EMPTY = "";
    public static final String APOSTROPHE = "'";
    public static final String DOLLAR = "$";
    public static final String HASH = "#";
    public static final String DASH = "-";
    public static final String DOT = ".";
    public static final String DOT_REGEX = "\\.";
    public static final String F_SLASH = "/";
    public static final String B_SLASH = "\\";
    public static final String F = "F";

    public static final String PRODUCT = "product";
    public static final String PRODUCT_AI = "product-ai";
    public static final String CATEGORY = "category";
    public static final String CATEGORY_AI = "category-ai";
    public static final String TAG = "tag";
    public static final String TAG_AI = "tag-ai";
    public static final String UX_BLOCK = "uxblock";
    public static final String UX_BLOCK_AI = "uxblock-ai";
    public static final String USER = "user";
    public static final String USER_CREATED_BY_SYSTEM = "system";
    public static final String USER_CREATED_BY_MANUAL = "manual";
    public static final String ORDER = "order";

    public static final String OXIDIZED_TAG = "#ox";
    public static final String COMBO_SET_CATEGORY = "#cs";
    public static final String SHORT_DESCRIPTION = "Short Description";
    public static final String OUT_OF_STOCK = "outofstock";
    public static final String IN_STOCK = "instock";
    public static final String TAXABLE = "taxable";
    public static final String STANDARD = "standard";
    public static final String SIMPLE = "simple";
    public static final String DRAFT = "draft";
    public static final String GLAMAYA = "Glamaya";
    public static final String BRAND = "Brand";
    public static final String COUNTRY_OF_ORIGIN = "Country of Origin";
    public static final String INDIA = "India";
    public static final String REFUND = "refund";
    public static final String IMAGE = "image";
    public static final String SKU_REGEX = "^GLAM-[A-Z]{2}-[A-Z]{2}-\\d*F?\\d-\\d{6}-\\d+-\\d+$";
    public static final String SKU_REGEX_EXISTS = "GLAM+-[A-Z]{2}-[A-Z]{2}-[1-9]+.*[0-9]+-[1-9][0-9]*-[1-9][0-9]*";

    public static final String NEW = "new";
    public static final String NO = "no";
    public static final String YES = "yes";
    public static final String MALE = "male";
    public static final String FEMALE = "female";
    public static final String UNISEX = "unisex";
    public static final String ADULT = "adult";
    public static final String KIDS = "kids";
    public static final String TODDLER = "toddler";
    public static final String INFANT = "infant";
    public static final String SENIOR = "senior";
    public static final String NEWBORN = "newborn";
    public static final String _ID = "_id";
    public static final String ID = "id";

    // Date and time related constants
    public static final String DATE_MODIFIED = "date_modified";
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final ZoneId IST_ZONE = ZoneId.of(ZoneId.SHORT_IDS.get("IST"));
    public static final Function<String, Instant> STRING_DATE_TO_INSTANT_FUNCTION = date ->
            Optional.ofNullable(date).filter(StringUtils::hasText)
                    .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                    .orElse(null);
    public static final Function<String, Instant> STRING_DATE_TO_INSTANT_IST_FUNCTION = date ->
            Optional.ofNullable(date).filter(StringUtils::hasText)
                    .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(IST_ZONE).toInstant())
                    .orElse(null);

    public static final String YITH_COG_COST_META = "yith_cog_cost";
    public static final String YOAST_WPSEO_METADESC_META = "_yoast_wpseo_metadesc";
    public static final String YOAST_WPSEO_FOCUSKW_META = "_yoast_wpseo_focuskw";
    public static final String YOAST_POST_REDIRECT_INFO_META = "_yoast_post_redirect_info";
    public static final String YOAST_WPSEO_FOCUSKEYWORDS_META = "_yoast_wpseo_focuskeywords";
    public static final String YOAST_WPSEO_KEYWORDSYNONYMS_META = "_yoast_wpseo_keywordsynonyms";
    public static final String WC_GLA_BRAND_META = "_wc_gla_brand";
    public static final String WC_GLA_CONDITION_META = "_wc_gla_condition";
    public static final String WC_GLA_ADULT_META = "_wc_gla_adult";
    public static final String WC_GLA_GENDER_META = "_wc_gla_gender";
    public static final String WC_GLA_AGE_GROUP_META = "_wc_gla_ageGroup";
    public static final String WC_FB_GENDER_META = "_wc_facebook_enhanced_catalog_attributes_gender";
    public static final String WC_FB_AGE_GROUP_META = "_wc_facebook_enhanced_catalog_attributes_age_group";
    public static final String WC_FB_GOOGLE_PRODUCT_CATEGORY_META = "_wc_facebook_google_product_category";

    public static final String ANKLETS = "anklets";
    public static final String BANGLES_BRACELETS_ARMLETS = "bangles-bracelets-armlets";
    public static final String BANGLES = "bangles";
    public static final String BANGLES_BRACELETS = "bangles-bracelets";
    public static final String BANJUBAND = "banjuband";
    public static final String BRACELETS = "bracelets";
    public static final String CHUDI = "chudi";
    public static final String HANDCUFFS = "handcuffs";
    public static final String KADA = "kada";
    public static final String COMBO_SETS = "combo-sets";
    public static final String EARRINGS = "earrings";
    public static final String BUGADI = "bugadi";
    public static final String DANGLER = "dangler";
    public static final String DROPLETS = "droplets";
    public static final String EARCLIPS = "earclips";
    public static final String EARCUFFS = "earcuffs";
    public static final String HOOPS = "hoops";
    public static final String JHUMKAS = "jhumkas";
    public static final String STUD = "stud";
    public static final String TASSEL = "tassel";
    public static final String HAIR_ACCESSORIES = "hair-accessories";
    public static final String HAIRPINS = "hairpins";
    public static final String MAANGTIKKA = "maangtikka";
    public static final String MATHAPATTI = "mathapatti";
    public static final String HATHFUL = "hathful";
    public static final String NECKLACES = "necklaces";
    public static final String CHOKER = "choker";
    public static final String HASLI = "hasli";
    public static final String PENDANTS = "pendants";
    public static final String RINGS = "rings";
    public static final String WAIST_BELT = "waist-belt";
    public static final String BROOCH = "brooch";
    public static final String JEWELLERY = "jewellery";
    public static final String NOSE_PINS = "nose-pins";
    public static final String CLIPON = "clipon";
    public static final String PIERCED = "pierced";
    public static final String PIERCED_NOSE_PINS = "pierced-nose-pins";
    public static final String SEPTUM = "septum";
    public static final String TOE_RINGS = "toe-rings";
    public static final String OTHERS_JEWELLERY = "others";
}
