
struct Lexer {
    tokens: List<Int>
    token_count: Int
    source: String
    cursor: Int
}

fun Lexer::new(source: String): Lexer {
    ret Lexer $[
        source: source,
        token_count: 0,
        tokens: List::new<Int>(),
        cursor: 0,
    ]
}

fun Lexer.add_token(ty: Int, offset: Int, extra: Int) {
    this.tokens[] = ty
    this.tokens[] = offset
    this.tokens[] = extra
    this.token_count = this.token_count + 1
}

fun Lexer.add_single_token(ty: Int) {
    this.add_token(ty, this.cursor, 0)
    this.cursor = this.cursor + 1
}

fun Lexer.get_token_type(index: Int): Int {
    ret this.tokens[index / 3]!!
}

fun Lexer.get_token_offset(index: Int): Int {
    ret this.tokens[index / 3 + 1]!!
}

fun Lexer.get_token_extra(index: Int): Int {
    ret this.tokens[index / 3 + 2]!!
}

fun Lexer.add_string() {
    this.add_token(TOKEN_TYPE_PLAIN_STRING, this.cursor, 0)
}

fun Lexer.add_identifier() {
    this.add_token(TOKEN_TYPE_IDENTIFIER, this.cursor, 0)
}

fun Lexer.add_number() {
    this.add_token(TOKEN_TYPE_INT_NUMBER, this.cursor, 0)
}

fun Lexer.process() {
    this.cursor = 0
    loop {
        if this.cursor >= this.source.byte_len() {
            ret nothing
        }

        let byte: Int = this.source.get_byte(this.cursor).to_int()

        while byte == 32 {
            this.cursor = this.cursor + 1

            if this.cursor >= this.source.byte_len() {
                ret nothing
            }

            byte = this.source.get_byte(this.cursor).to_int()
        }

        when(byte) {
            /* ! */ 33 -> this.add_single_token(TOKEN_TYPE_NOT)
            /* " */ 34 -> this.add_string()
            /* # */ 35 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* $ */ 36 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* % */ 37 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* & */ 38 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ' */ 39 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ( */ 40 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ) */ 41 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* * */ 42 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* + */ 43 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* , */ 44 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* - */ 45 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* . */ 46 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* / */ 47 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* 0 */ 48 -> this.add_number()
            /* 1 */ 49 -> this.add_number()
            /* 2 */ 50 -> this.add_number()
            /* 3 */ 51 -> this.add_number()
            /* 4 */ 52 -> this.add_number()
            /* 5 */ 53 -> this.add_number()
            /* 6 */ 54 -> this.add_number()
            /* 7 */ 55 -> this.add_number()
            /* 8 */ 56 -> this.add_number()
            /* 9 */ 57 -> this.add_number()
            /* : */ 58 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ; */ 59 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* < */ 60 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* = */ 61 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* > */ 62 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ? */ 63 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* @ */ 64 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* A */ 65 -> this.add_identifier()
            /* B */ 66 -> this.add_identifier()
            /* C */ 67 -> this.add_identifier()
            /* D */ 68 -> this.add_identifier()
            /* E */ 69 -> this.add_identifier()
            /* F */ 70 -> this.add_identifier()
            /* G */ 71 -> this.add_identifier()
            /* H */ 72 -> this.add_identifier()
            /* I */ 73 -> this.add_identifier()
            /* J */ 74 -> this.add_identifier()
            /* K */ 75 -> this.add_identifier()
            /* L */ 76 -> this.add_identifier()
            /* M */ 77 -> this.add_identifier()
            /* N */ 78 -> this.add_identifier()
            /* O */ 79 -> this.add_identifier()
            /* P */ 80 -> this.add_identifier()
            /* Q */ 81 -> this.add_identifier()
            /* R */ 82 -> this.add_identifier()
            /* S */ 83 -> this.add_identifier()
            /* T */ 84 -> this.add_identifier()
            /* U */ 85 -> this.add_identifier()
            /* V */ 86 -> this.add_identifier()
            /* W */ 87 -> this.add_identifier()
            /* X */ 88 -> this.add_identifier()
            /* Y */ 89 -> this.add_identifier()
            /* Z */ 90 -> this.add_identifier()
            /* [ */ 91 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* \ */ 92 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ] */ 93 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ^ */ 94 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* _ */ 95 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ` */ 96 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* a */ 97 -> this.add_identifier()
            /* b */ 98 -> this.add_identifier()
            /* c */ 99 -> this.add_identifier()
            /* d */ 100 -> this.add_identifier()
            /* e */ 101 -> this.add_identifier()
            /* f */ 102 -> this.add_identifier()
            /* g */ 103 -> this.add_identifier()
            /* h */ 104 -> this.add_identifier()
            /* i */ 105 -> this.add_identifier()
            /* j */ 106 -> this.add_identifier()
            /* k */ 107 -> this.add_identifier()
            /* l */ 108 -> this.add_identifier()
            /* m */ 109 -> this.add_identifier()
            /* n */ 110 -> this.add_identifier()
            /* o */ 111 -> this.add_identifier()
            /* p */ 112 -> this.add_identifier()
            /* q */ 113 -> this.add_identifier()
            /* r */ 114 -> this.add_identifier()
            /* s */ 115 -> this.add_identifier()
            /* t */ 116 -> this.add_identifier()
            /* u */ 117 -> this.add_identifier()
            /* v */ 118 -> this.add_identifier()
            /* w */ 119 -> this.add_identifier()
            /* x */ 120 -> this.add_identifier()
            /* y */ 121 -> this.add_identifier()
            /* z */ 122 -> this.add_identifier()
            /* { */ 123 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* | */ 124 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* } */ 125 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            /* ~ */ 126 -> this.add_single_token(TOKEN_TYPE_RPAREN)
            else -> this.add_single_token(TOKEN_TYPE_ERROR_CHARACTER)
        }
    }
}

fun Lexer.debug_print() {
    println("Lexer \$[token_count: ${this.token_count}, cursor: ${this.cursor}]")
    repeat this.token_count {
        println(" - ${this.get_token_offset(it)}: ${this.get_token_type(it)}")
    }
}

let MODE_NORMAL: Int  = 0
let MODE_STRING: Int  = 1
let MODE_BLOCK: Int   = 2

let TOKEN_TYPE_RANGE_IN: Int            = 0
let TOKEN_TYPE_RANGE_EX: Int            = 1
let TOKEN_TYPE_LAMBDA_START: Int        = 2
let TOKEN_TYPE_SET_START: Int           = 3
let TOKEN_TYPE_MAP_START: Int           = 4
let TOKEN_TYPE_LIST_START: Int          = 5
let TOKEN_TYPE_STRUCT_START: Int        = 6
let TOKEN_TYPE_STRING_INTERP: Int       = 7
let TOKEN_TYPE_DOUBLE_COLON: Int        = 8
let TOKEN_TYPE_ARROW: Int               = 9
let TOKEN_TYPE_OROR: Int                = 10
let TOKEN_TYPE_ANDAND: Int              = 11
let TOKEN_TYPE_XORXOR: Int              = 12
let TOKEN_TYPE_COMPARE: Int             = 13
let TOKEN_TYPE_EQ: Int                  = 14
let TOKEN_TYPE_NEQ: Int                 = 15
let TOKEN_TYPE_LEQ: Int                 = 16
let TOKEN_TYPE_GEQ: Int                 = 17
let TOKEN_TYPE_DOT: Int                 = 18
let TOKEN_TYPE_LPAREN: Int              = 19
let TOKEN_TYPE_RPAREN: Int              = 20
let TOKEN_TYPE_LBRACE: Int              = 21
let TOKEN_TYPE_LBRACKET: Int            = 22
let TOKEN_TYPE_RBRACKET: Int            = 23
let TOKEN_TYPE_COMMA: Int               = 24
let TOKEN_TYPE_COLON: Int               = 25
let TOKEN_TYPE_ADD: Int                 = 26
let TOKEN_TYPE_SUB: Int                 = 27
let TOKEN_TYPE_MUL: Int                 = 28
let TOKEN_TYPE_DIV: Int                 = 29
let TOKEN_TYPE_XOR: Int                 = 30
let TOKEN_TYPE_DOLAR: Int               = 31
let TOKEN_TYPE_MOD: Int                 = 32
let TOKEN_TYPE_AT: Int                  = 33
let TOKEN_TYPE_HASH: Int                = 34
let TOKEN_TYPE_OR: Int                  = 35
let TOKEN_TYPE_AND: Int                 = 36
let TOKEN_TYPE_NOT: Int                 = 37
let TOKEN_TYPE_ASSIGN: Int              = 38
let TOKEN_TYPE_LTH: Int                 = 39
let TOKEN_TYPE_GTH: Int                 = 40
let TOKEN_TYPE_THIS: Int                = 41
let TOKEN_TYPE_THIS_TYPE: Int           = 42
let TOKEN_TYPE_FUN: Int                 = 43
let TOKEN_TYPE_LET: Int                 = 44
let TOKEN_TYPE_MODULE: Int              = 45
let TOKEN_TYPE_STRUCT: Int              = 46
let TOKEN_TYPE_RETURN: Int              = 47
let TOKEN_TYPE_SIZE_OF: Int             = 48
let TOKEN_TYPE_PTR_OF: Int              = 49
let TOKEN_TYPE_MEMORY_WRITE: Int        = 50
let TOKEN_TYPE_MEMORY_READ: Int         = 51
let TOKEN_TYPE_OPTION: Int              = 52
let TOKEN_TYPE_REC: Int                 = 53
let TOKEN_TYPE_TAG: Int                 = 54
let TOKEN_TYPE_NOTHING: Int             = 55
let TOKEN_TYPE_WHEN: Int                = 56
let TOKEN_TYPE_MATCH: Int               = 57
let TOKEN_TYPE_EITHER: Int              = 58
let TOKEN_TYPE_ALIAS: Int               = 59
let TOKEN_TYPE_IF: Int                  = 60
let TOKEN_TYPE_ELSE: Int                = 61
let TOKEN_TYPE_FOR: Int                 = 62
let TOKEN_TYPE_IN: Int                  = 63
let TOKEN_TYPE_NOT_IN: Int              = 64
let TOKEN_TYPE_WHILE: Int               = 65
let TOKEN_TYPE_REPEAT: Int              = 66
let TOKEN_TYPE_LOOP: Int                = 67
let TOKEN_TYPE_IS: Int                  = 68
let TOKEN_TYPE_NOT_IS: Int              = 69
let TOKEN_TYPE_AS: Int                  = 70
let TOKEN_TYPE_TRUE: Int                = 71
let TOKEN_TYPE_FALSE: Int               = 72
let TOKEN_TYPE_NULL: Int                = 73
let TOKEN_TYPE_INCLUDE: Int             = 74
let TOKEN_TYPE_BREAK: Int               = 75
let TOKEN_TYPE_CONTINUE: Int            = 76
let TOKEN_TYPE_JSON: Int                = 77
let TOKEN_TYPE_USE: Int                 = 78
let TOKEN_TYPE_MUT: Int                 = 79
let TOKEN_TYPE_REF: Int                 = 80
let TOKEN_TYPE_REF_MUT: Int             = 81
let TOKEN_TYPE_COPY: Int                = 82
let TOKEN_TYPE_BLOCK_START: Int         = 83
let TOKEN_TYPE_WHITE_SPACE: Int         = 84
let TOKEN_TYPE_NL: Int                  = 85
let TOKEN_TYPE_LINE_COMMENT: Int        = 86
let TOKEN_TYPE_DOC_COMMENT: Int         = 87
let TOKEN_TYPE_BLOCK_COMMENT: Int       = 88
let TOKEN_TYPE_INT_NUMBER: Int          = 89
let TOKEN_TYPE_FLOAT_NUMBER: Int        = 90
let TOKEN_TYPE_IDENTIFIER: Int          = 91
let TOKEN_TYPE_PLAIN_STRING: Int        = 92
let TOKEN_TYPE_RBRACE: Int              = 93
let TOKEN_TYPE_STRING_ESCAPE: Int       = 94
let TOKEN_TYPE_STRING_INTERP_START: Int = 95
let TOKEN_TYPE_STRING_INTERP_END: Int   = 96
let TOKEN_TYPE_STRING_VAR: Int          = 97
let TOKEN_TYPE_STRING_BLOB: Int         = 98
let TOKEN_TYPE_STRING_END: Int          = 99
let TOKEN_TYPE_BLOCK_END: Int           = 100
let TOKEN_TYPE_BLOCK_BLOB: Int          = 101
let TOKEN_TYPE_BLOCK_OTHER: Int         = 102
let TOKEN_TYPE_ERROR_CHARACTER: Int     = 255