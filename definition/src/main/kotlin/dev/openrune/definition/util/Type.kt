package dev.openrune.definition.util

import dev.openrune.definition.serialization.TypeSerializer
import kotlinx.serialization.Serializable

private const val NO_KEY = 0.toChar()

@Serializable(with = TypeSerializer::class)
enum class Type {
    INT(0, 'i', BaseVarType.INTEGER),
    BOOLEAN(1, '1', BaseVarType.INTEGER),
    HASH32(2, '2', BaseVarType.INTEGER),
    QUEST(3, ':', BaseVarType.INTEGER),
    QUESTHELP(4, ';', BaseVarType.INTEGER),
    CURSOR(5, '@', BaseVarType.INTEGER),
    SEQ(6, 'A', BaseVarType.INTEGER),
    COLOUR(7, 'C', BaseVarType.INTEGER),
    LOC_SHAPE(8, 'H', BaseVarType.INTEGER, "locshape"),
    COMPONENT(9, 'I', BaseVarType.INTEGER),
    IDKIT(10, 'K', BaseVarType.INTEGER),
    MIDI(11, 'M', BaseVarType.INTEGER),
    NPC_MODE(12, 'N', BaseVarType.INTEGER),
    NAMEDOBJ(13, 'O', BaseVarType.INTEGER),
    SYNTH(14, 'P', BaseVarType.INTEGER),
    AI_QUEUE(15, 'Q', BaseVarType.INTEGER),
    AREA(16, 'R', BaseVarType.INTEGER),
    STAT(17, 'S', BaseVarType.INTEGER),
    NPC_STAT(18, 'T', BaseVarType.INTEGER),
    WRITEINV(19, 'V', BaseVarType.INTEGER),
    MESH(20, '^', BaseVarType.INTEGER),
    MAPAREA(21, '`', BaseVarType.INTEGER, "wma"),
    COORDGRID(22, 'c', BaseVarType.INTEGER, "coord"),
    GRAPHIC(23, 'd', BaseVarType.INTEGER),
    CHATPHRASE(24, 'e', BaseVarType.INTEGER),
    FONTMETRICS(25, 'f', BaseVarType.INTEGER),
    ENUM(26, 'g', BaseVarType.INTEGER),
    HUNT(27, 'h', BaseVarType.INTEGER),
    JINGLE(28, 'j', BaseVarType.INTEGER),
    CHATCAT(29, 'k', BaseVarType.INTEGER),
    LOC(30, 'l', BaseVarType.INTEGER),
    MODEL(31, 'm', BaseVarType.INTEGER),
    NPC(32, 'n', BaseVarType.INTEGER),
    OBJ(33, 'o', BaseVarType.INTEGER),  // namedobj
    PLAYER_UID(34, 'p', BaseVarType.INTEGER),
    REGION_UID(35, 'r', BaseVarType.LONG),  // todo: this integer in osrs java client
    STRING(36, 's', BaseVarType.STRING),
    SPOTANIM(37, 't', BaseVarType.INTEGER),
    NPC_UID(38, 'u', BaseVarType.INTEGER),
    INV(39, 'v', BaseVarType.INTEGER),
    TEXTURE(40, 'x', BaseVarType.INTEGER),
    CATEGORY(41, 'y', BaseVarType.INTEGER),
    CHAR(42, 'z', BaseVarType.INTEGER),
    LASER(43, '|', BaseVarType.INTEGER),
    BAS(44, '€', BaseVarType.INTEGER),
    CONTROLLER(45, 'ƒ', BaseVarType.INTEGER),
    COLLISION_GEOMETRY(46, '‡', BaseVarType.INTEGER),
    PHYSICS_MODEL(47, '‰', BaseVarType.INTEGER),
    PHYSICS_CONTROL_MODIFIER(48, 'Š', BaseVarType.INTEGER),
    CLANHASH(49, 'Œ', BaseVarType.LONG),
    CUTSCENE(51, 'š', BaseVarType.INTEGER),
    ITEMCODE(53, '¡', BaseVarType.INTEGER),
    PVPKILLS(54, '¢', BaseVarType.INTEGER),
    MAPSCENEICON(55, '£', BaseVarType.INTEGER, "msi"),
    CLANFORUMQFC(56, '§', BaseVarType.LONG),
    VORBIS(57, '«', BaseVarType.INTEGER),
    VERIFY_OBJECT(58, '®', BaseVarType.INTEGER, "verifyobj"),
    MAPELEMENT(59, 'µ', BaseVarType.INTEGER),
    CATEGORYTYPE(60, '¶', BaseVarType.INTEGER),
    SOCIAL_NETWORK(61, 'Æ', BaseVarType.INTEGER, "socialnetwork"),
    HITMARK(62, '×', BaseVarType.INTEGER),
    PACKAGE(63, 'Þ', BaseVarType.INTEGER),
    PARTICLE_EFFECTOR(64, 'á', BaseVarType.INTEGER, "pef"),
    CONTROLLER_UID(65, 'æ', BaseVarType.INTEGER),
    PARTICLE_EMITTER(66, 'é', BaseVarType.INTEGER, "pem"),
    PLOGTYPE(67, 'í', BaseVarType.INTEGER, "plog"),
    UNSIGNED_INT(68, 'î', BaseVarType.INTEGER),
    SKYBOX(69, 'ó', BaseVarType.INTEGER),
    SKYDECOR(70, 'ú', BaseVarType.INTEGER),
    HASH64(71, 'û', BaseVarType.LONG),
    INPUTTYPE(72, 'Î', BaseVarType.INTEGER),
    STRUCT(73, 'J', BaseVarType.INTEGER),
    DBROW(74, 'Ð', BaseVarType.INTEGER),
    STORABLELABEL(75, '¤', BaseVarType.INTEGER),
    STORABLEPROC(76, '¥', BaseVarType.INTEGER),
    GAMELOGEVENT(77, 'è', BaseVarType.INTEGER),
    ANIMATIONCLIP(78, '¹', BaseVarType.INTEGER),
    SKELETON(79, '°', BaseVarType.INTEGER),
    REGIONVISIBILITY(80, 'ì', BaseVarType.INTEGER, "region_visibility"),
    FMODHANDLE(81, 'ë', BaseVarType.INTEGER),
    REGION_ALLOWLOGIN(83, 'þ', BaseVarType.INTEGER),
    REGION_INFO(84, 'ý', BaseVarType.INTEGER),
    REGION_INFO_FAILURE(85, 'ÿ', BaseVarType.INTEGER),
    SERVER_ACCOUNT_CREATION_STEP(86, 'õ', BaseVarType.INTEGER),
    CLIENT_ACCOUNT_CREATION_STEP(87, 'ô', BaseVarType.INTEGER),
    LOBBY_ACCOUNT_CREATION_STEP(88, 'ö', BaseVarType.INTEGER),
    GWC_PLATFORM(89, 'ò', BaseVarType.INTEGER, "gwc_platform"),
    CURRENCY(90, 'Ü', BaseVarType.INTEGER),
    KEYBOARD_KEY(91, 'ù', BaseVarType.INTEGER),
    MOUSEEVENT(92, 'ï', BaseVarType.INTEGER),
    HEADBAR(93, '¯', BaseVarType.INTEGER),
    BUG_TEMPLATE(94, 'ê', BaseVarType.INTEGER, "bugtemplate"),
    BILLING_AUTH_FLAG(95, 'ð', BaseVarType.INTEGER, "billingauthflag"),
    ACCOUNT_FEATURE_FLAG(96, 'å', BaseVarType.INTEGER, "accountfeatureflag"),
    INTERFACE(97, 'a', BaseVarType.INTEGER),
    TOPLEVELINTERFACE(98, 'F', BaseVarType.INTEGER),
    OVERLAYINTERFACE(99, 'L', BaseVarType.INTEGER),
    CLIENTINTERFACE(100, '©', BaseVarType.INTEGER),
    MOVESPEED(101, 'Ý', BaseVarType.INTEGER),
    MATERIAL(102, '¬', BaseVarType.INTEGER),
    SEQGROUP(103, 'ø', BaseVarType.INTEGER),
    TEMP_HISCORE(104, 'ä', BaseVarType.INTEGER, "temphiscore"),
    TEMP_HISCORE_LENGTH_TYPE(105, 'ã', BaseVarType.INTEGER, "temphiscorelengthtype"),
    TEMP_HISCORE_DISPLAY_TYPE(106, 'â', BaseVarType.INTEGER, "temphiscoretype"),
    TEMP_HISCORE_CONTRIBUTE_RESULT(107, 'à', BaseVarType.INTEGER, "temphiscorecontributeresult"),
    AUDIOGROUP(108, 'À', BaseVarType.INTEGER),
    AUDIOMIXBUSS(109, 'Ò', BaseVarType.INTEGER, "audiobuss"),
    LONG(110, 'Ï', BaseVarType.LONG),
    CRM_CHANNEL(111, 'Ì', BaseVarType.INTEGER),
    HTTP_IMAGE(112, 'É', BaseVarType.INTEGER),
    POP_UP_DISPLAY_BEHAVIOUR(113, 'Ê', BaseVarType.INTEGER, "popupdisplaybehaviour"),
    POLL(114, '÷', BaseVarType.INTEGER),
    MTXN_PACKAGE(115, '¼', BaseVarType.LONG),
    MTXN_PRICE_POINT(116, '½', BaseVarType.LONG),
    ENTITYOVERLAY(117, '-', BaseVarType.INTEGER),
    DBTABLE(118, 'Ø', BaseVarType.INTEGER),

    // Group 2
    COMPONENTARRAY(200, 'X', BaseVarType.INTEGER),
    INTARRAY(201, 'W', BaseVarType.INTEGER),
    LABEL(202, 'b', BaseVarType.INTEGER),
    QUEUE(203, 'B', BaseVarType.INTEGER),
    TIMER(204, '4', BaseVarType.INTEGER),
    WEAKQUEUE(205, 'w', BaseVarType.INTEGER),
    SOFTTIMER(206, 'q', BaseVarType.INTEGER),
    OBJVAR(207, '0', BaseVarType.INTEGER),
    WALKTRIGGER(208, '6', BaseVarType.INTEGER),
    VARP(209, '7', BaseVarType.INTEGER),  // 214

    // Group 3
    TYPE_SPECIAL_1(-1, '#', BaseVarType.INTEGER),
    TYPE_SPECIAL_2(-1, '(', BaseVarType.INTEGER),
    TYPE_SPECIAL_3(-1, '%', BaseVarType.INTEGER),
    TYPE_SPECIAL_4(-1, '&', BaseVarType.INTEGER),
    TYPE_SPECIAL_5(-1, ')', BaseVarType.INTEGER),
    TYPE_SPECIAL_6(-1, '3', BaseVarType.INTEGER),
    TYPE_SPECIAL_7(-1, '5', BaseVarType.INTEGER),
    TYPE_SPECIAL_8(-1, '7', BaseVarType.INTEGER),
    TYPE_SPECIAL_9(-1, '8', BaseVarType.INTEGER),
    TYPE_SPECIAL_10(-1, '9', BaseVarType.INTEGER),
    TYPE_SPECIAL_11(-1, 'D', BaseVarType.INTEGER),
    TYPE_SPECIAL_12(-1, 'G', BaseVarType.INTEGER),
    TYPE_SPECIAL_13(-1, 'U', BaseVarType.INTEGER),
    TYPE_SPECIAL_14(-1, 'Á', BaseVarType.INTEGER),
    TYPE_SPECIAL_15(-1, 'Z', BaseVarType.INTEGER),
    TYPE_SPECIAL_16(-1, '~', BaseVarType.INTEGER),
    TYPE_SPECIAL_17(-1, '±', BaseVarType.INTEGER),
    TYPE_SPECIAL_18(-1, '»', BaseVarType.INTEGER),
    TYPE_SPECIAL_19(-1, '¿', BaseVarType.INTEGER),
    TYPE_SPECIAL_20(-1, 'Ç', BaseVarType.INTEGER),
    TYPE_SPECIAL_21(-1, 'Ñ', BaseVarType.INTEGER),
    TYPE_SPECIAL_22(-1, 'ñ', BaseVarType.INTEGER),
    TYPE_SPECIAL_23(-1, 'Ù', BaseVarType.INTEGER),
    TYPE_SPECIAL_24(-1, 'ß', BaseVarType.INTEGER),
    TYPE_SPECIAL_25(-1, 'E', BaseVarType.INTEGER),
    TRANSMIT_LIST(-1, 'Y', BaseVarType.INTEGER),
    TYPE_SPECIAL_27(-1, 'Ä', BaseVarType.INTEGER),
    TYPE_SPECIAL_28(-1, 'ü', BaseVarType.INTEGER),
    TYPE_SPECIAL_29(-1, 'Ú', BaseVarType.INTEGER),
    TYPE_SPECIAL_30(-1, 'Û', BaseVarType.INTEGER),
    TYPE_SPECIAL_31(-1, 'Ó', BaseVarType.INTEGER),
    TYPE_SPECIAL_32(-1, 'È', BaseVarType.INTEGER),
    TYPE_SPECIAL_33(-1, 'Ô', BaseVarType.INTEGER),
    TYPE_SPECIAL_34(-1, '¾', BaseVarType.INTEGER),
    TYPE_SPECIAL_35(-1, 'Ö', BaseVarType.INTEGER),
    TYPE_SPECIAL_36(-1, '³', BaseVarType.INTEGER),
    TYPE_SPECIAL_37(-1, '·', BaseVarType.INTEGER),
    TYPE_SPECIAL_38(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_39(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_40(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_41(-1, 'º', BaseVarType.INTEGER),
    TYPE_SPECIAL_42(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_43(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_44(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_45(-1, NO_KEY, BaseVarType.INTEGER),
    TYPE_SPECIAL_46(-1, '!', null),
    TYPE_SPECIAL_47(-1, '$', null),
    TYPE_SPECIAL_48(-1, '?', null),
    TYPE_SPECIAL_49(-1, 'ç', null),
    TYPE_SPECIAL_50(-1, '*', null),

    // unknown id
    STRINGVECTOR(-1, '¸', BaseVarType.INTEGER),  // added in 202 todo: id?

    // unknown id - server only
    MESANIM,
    UNDERLAY,
    OVERLAY,
    WORLD_AREA,

    // special
    TYPE(-1, NO_KEY, BaseVarType.INTEGER),
    BASEVARTYPE(-1, NO_KEY, BaseVarType.INTEGER),
    PARAM(-1, NO_KEY, BaseVarType.INTEGER),
    CLIENTSCRIPT(-1, NO_KEY, BaseVarType.INTEGER),
    ONSHIFTCLICKNPC(-1, NO_KEY, BaseVarType.INTEGER),
    ONSHIFTCLICKLOC(-1, NO_KEY, BaseVarType.INTEGER),
    ONSHIFTCLICKOBJ(-1, NO_KEY, BaseVarType.INTEGER),
    ONSHIFTCLICKPLAYER(-1, NO_KEY, BaseVarType.INTEGER),
    ONSHIFTCLICKTILE(-1, NO_KEY, BaseVarType.INTEGER),
    DBCOLUMN(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_PLAYER(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_PLAYER_BIT(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CLIENT(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CLIENT_STRING(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CLAN_SETTING(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CLAN(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CONTROLLER(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_CONTROLLER_BIT(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_GLOBAL(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_NPC(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_NPC_BIT(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_OBJ(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_SHARED(-1, NO_KEY, BaseVarType.INTEGER),
    VAR_SHARED_STRING(-1, NO_KEY, BaseVarType.INTEGER),

    // split the int type into fake subtypes
    INT_INT(-1, NO_KEY, BaseVarType.INTEGER, INT, "int"),
    INT_BOOLEAN(-1, NO_KEY, BaseVarType.INTEGER, INT, "intbool"),
    INT_CHATFILTER(-1, NO_KEY, BaseVarType.INTEGER, INT, "chatfilter"),
    INT_CHATTYPE(-1, NO_KEY, BaseVarType.INTEGER, INT, "chattype"),
    INT_CLIENTTYPE(-1, NO_KEY, BaseVarType.INTEGER, INT, "clienttype"),
    INT_PLATFORMTYPE(-1, NO_KEY, BaseVarType.INTEGER, INT, "platformtype"),
    INT_IFTYPE(-1, NO_KEY, BaseVarType.INTEGER, INT, "iftype"),
    INT_KEY(-1, NO_KEY, BaseVarType.INTEGER, INT, "key"),
    INT_SETPOSH(-1, NO_KEY, BaseVarType.INTEGER, INT, "setposh"),
    INT_SETPOSV(-1, NO_KEY, BaseVarType.INTEGER, INT, "setposv"),
    INT_SETSIZE(-1, NO_KEY, BaseVarType.INTEGER, INT, "setsize"),
    INT_SETTEXTALIGNH(-1, NO_KEY, BaseVarType.INTEGER, INT, "settextalignh"),
    INT_SETTEXTALIGNV(-1, NO_KEY, BaseVarType.INTEGER, INT, "settextalignv"),
    INT_WINDOWMODE(-1, NO_KEY, BaseVarType.INTEGER, INT, "windowmode"),
    INT_GAMEOPTION(-1, NO_KEY, BaseVarType.INTEGER, INT, "gameoption"),
    INT_DEVICEOPTION(-1, NO_KEY, BaseVarType.INTEGER, INT, "deviceoption"),
    INT_MENUENTRYTYPE(-1, NO_KEY, BaseVarType.INTEGER, INT, "menuentrytype"),
    INT_GRADIENTMODE(-1, NO_KEY, BaseVarType.INTEGER, INT, "gradientmode"),
    INT_OBJOWNER(-1, NO_KEY, BaseVarType.INTEGER, INT, "objowner"),
    INT_RGB(-1, NO_KEY, BaseVarType.INTEGER, INT, "rgb"),
    INT_OPKIND(-1, NO_KEY, BaseVarType.INTEGER, INT, "opkind"),
    INT_OPMODE(-1, NO_KEY, BaseVarType.INTEGER, INT, "opmode"),

    // for decompiler
    HOOK,
    UNKNOWN,
    UNKNOWN_INT,  // int-based
    UNKNOWN_INT_NOTBOOLEAN,  // int-based, boolean impossible based on value set
    UNKNOWN_INT_NOTINT,  // int-based, int impossible based on default return -1
    UNKNOWN_INT_NOTINT_NOTBOOLEAN,  // int-based, both int and boolean impossible
    CONDITION,
    ;

    val fullName: String
    val id: Int
    val ch: Char
    val baseType: BaseVarType?
    val alias: Type?

    constructor(id: Int, ch: Char, baseType: BaseVarType?) {
        this.id = id
        this.ch = ch
        this.baseType = baseType
        this.fullName = name.lowercase()
        alias = null
    }

    constructor(id: Int, ch: Char, baseType: BaseVarType, name: String) {
        this.id = id
        this.ch = ch
        this.baseType = baseType
        this.fullName = name
        alias = null
    }

    constructor(id: Int, ch: Char, baseType: BaseVarType, alias: Type, name: String) {
        this.id = id
        this.ch = ch
        this.baseType = baseType
        this.alias = alias
        this.fullName = name
    }

    constructor() {
        id = -1
        ch = NO_KEY
        baseType = null
        this.fullName = name.lowercase()
        alias = null
    }

    companion object {
        fun byID(id: Int): Type {
            for (value in entries) {
                if (value.id == id) {
                    return value
                }
            }

            throw IllegalArgumentException("unknown id $id")
        }

        fun byChar(id: Char): Type {
            for (value in entries) {
                if (value.ch == id) {
                    return value
                }
            }

            throw IllegalArgumentException("unknown char $id")
        }

        // todo: clean this up, just define the primitive subtypes and take reflexive transitive closure
        fun subtype(a: Type, b: Type): Boolean {
            if (a == b) {
                return true
            }

            if (b == UNKNOWN) {
                return true
            }

            if (b == UNKNOWN_INT) {
                return a.baseType == BaseVarType.INTEGER || a == UNKNOWN_INT_NOTBOOLEAN || a == UNKNOWN_INT_NOTINT || a == UNKNOWN_INT_NOTINT_NOTBOOLEAN
            }

            if (b == UNKNOWN_INT_NOTBOOLEAN) {
                return a.baseType == BaseVarType.INTEGER && a != BOOLEAN || a == UNKNOWN_INT_NOTINT_NOTBOOLEAN
            }

            if (b == UNKNOWN_INT_NOTINT) {
                return a.baseType == BaseVarType.INTEGER && !subtype(a, INT) || a == UNKNOWN_INT_NOTINT_NOTBOOLEAN
            }

            if (b == UNKNOWN_INT_NOTINT_NOTBOOLEAN) {
                return a.baseType == BaseVarType.INTEGER && !subtype(a, INT) && a != BOOLEAN
            }

            if (a == OBJ && b == NAMEDOBJ) { // todo: return has different behavior
                return true
            }

            if (a.alias == b) {
                return true
            }

            if (a == INT_INT && b.alias == INT) {
                return true
            }

            return false
        }

        fun meet(typeA: Type, typeB: Type): Type? {
            if (subtype(typeA, typeB)) {
                return typeA
            }

            if (subtype(typeB, typeA)) {
                return typeB
            }

            if (typeA.alias == INT && typeB.alias == INT) {
                return INT_INT
            }

            if (typeA == UNKNOWN_INT_NOTBOOLEAN && typeB == UNKNOWN_INT_NOTINT || typeA == UNKNOWN_INT_NOTINT && typeB == UNKNOWN_INT_NOTBOOLEAN) {
                return UNKNOWN_INT_NOTINT_NOTBOOLEAN
            }

            return null
        }
    }
}