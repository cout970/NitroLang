// noinspection RegExpRedundantEscape,RegExpUnnecessaryNonCapturingGroup,RegExpSingleCharAlternation,JSFileReferences
"use strict";

import themeCss from '../res/css/theme.scss?raw';

// See https://ace.c9.io/tool/mode_creator.html
// https://ace.c9.io/#nav=higlighter

ace.define('ace/mode/nitro_highlight_rules', ['require', 'exports', 'ace/lib/oop', 'ace/mode/text_highlight_rules'], (acequire, exports) => {
  const oop = acequire('ace/lib/oop');
  const TextHighlightRules = acequire('ace/mode/text_highlight_rules').TextHighlightRules;

  const NitroHighlightRules = function NitroHighlightRules() {
    // regexp must not have capturing parentheses. Use (?:) instead.
    // regexps are ordered -> the first match is used

    const keywordMapper = this.$keywords = this.createKeywordMapper({
      "keyword.nitro": "Self|function|var|val|module|class|type|recv|receiver|trait|interface|either|ref_mut|ref|copy|or|and|xor|not|This|fun|let|mod|struct|ret|return|size_of|sizeOf|sizeof|option|rec|tag|defer|type_alias|typeAlias|typealias|enum|when|match|alias|if|else|for|in|while|repeat|loop|is|as|include|break|continue|use|mut",
      "keyword.constant.nitro": "true|false|null|this|nothing|self",
    }, "variable.nitro");

    const restart = (currentState, stack) => {
      while (stack && stack.length > 0) {
        stack.shift();
      }
      return "start";
    };

    const restart_to = (to_state) => {
      return (currentState, stack) => {
        while (stack && stack.length > 0 && stack[stack.length - 1] !== to_state) {
          stack.shift();
        }
        return to_state;
      };
    }

    this.$rules = {
      "start": [
        {
          include: "#comments"
        },
        {
          include: "#annotation"
        },
        {
          include: "#struct"
        },
        {
          include: "#option"
        },
        {
          include: "#tag"
        },
        {
          include: "#function_header"
        },
        {
          include: "#statements"
        },
        {
          include: "#expressions"
        },
        {
          token: "paren.lparen",
          regex: /[{(\[]/
        },
        {
          token: "paren.rparen",
          regex: /[})\]]/
        }
      ],
      "#comments": [
        {
          token: "comment",
          regex: /\/\*/,
          push: [
            {
              token: "comment",
              regex: /\*\//,
              next: "pop"
            },
            {
              defaultToken: "comment"
            }
          ]
        },
        {
          token: ["text", "comment"],
          regex: /(\s*)(\/\/.*$)/
        }
      ],
      "#annotation": [
        {
          token: ["storage.type.annotation.nitro", "text", "punctuation.annotation.start"],
          regex: /(@\w+)(\s*)(\[)/,
          push: [
            {
              token: "punctuation.annotation.end",
              regex: /\]/,
              next: restart,
            },
            {
              include: "#instantiation_field"
            },
            {
              token: "punctuation",
              regex: /,/
            },
            {
              token: "text",
              regex: /\s+/
            },
          ],
        },
        {
          token: "storage.type.annotation.nitro",
          regex: /@\w+\b/,
        }
      ],
      "#numbers": [
        {
          token: "constant.numeric.nitro",
          regex: /\b(?:0(?:[xbo])[0-9a-fA-F]*|(?:[0-9]+\.?[0-9]*|\.[0-9]+)(?:(?:[eE])(?:\+|-)?[0-9]+)?)(?:[LlFfUuDd]|UL|ul)?\b/
        },
      ],
      "#expressions": [
        {
          include: "#strings"
        },
        {
          include: "#keywords"
        },
        {
          include: "#function_calls"
        },
        {
          include: "#operators"
        },
        {
          include: "#numbers"
        },
        {
          token: keywordMapper,
          regex: /\b[a-z]\w*\b/
        },
        {
          token: "entity.name.variable.constant.nitro",
          regex: /\b[A-Z]+\b/
        },
        {
          token: "entity.name.variable.type.nitro",
          regex: /\b[A-Z]\w*\b/
        },
        {
          include: "#comments"
        },
      ],
      "#generics": [
        {
          token: "punctuation",
          regex: /</,
          push: [
            {
              token: "punctuation",
              regex: />/,
              next: "pop"
            },
            {
              token: "storage.type.generic.nitro",
              regex: /#\w+\b/
            },
            {
              token: "keyword.operator",
              regex: /:/
            },
            {
              token: "punctuation",
              regex: /,/
            },
            {
              include: "#generics"
            }
          ]
        }
      ],
      "#definition_generics": [
        {
          token: "punctuation.definition_generics.start",
          regex: /</,
          push: [
            {
              token: "punctuation.definition_generics.end",
              regex: />/,
              next: "pop"
            },
            {
              token: "storage.type.generic.nitro",
              regex: /#\w+\b/
            },
            {
              token: "punctuation",
              regex: /,/
            },
            {
              token: "punctuation",
              regex: /:/
            },
            {
              token: "storage.type.regular.nitro",
              regex: /\w+\b/
            },
            {
              token: "text",
              regex: /\s+/
            }
          ],
        }
      ],
      "#usage_generics": [
        {
          token: "punctuation.definition_generics.start",
          regex: /</,
          push: [
            {
              token: "punctuation.definition_generics.end",
              regex: />/,
              next: "pop"
            },
            {
              include: "#types",
            },
            {
              token: "punctuation",
              regex: /,/
            },
            {
              token: "text",
              regex: /\s+/
            }
          ],
        }
      ],
      "#types": [
        {
          include: "#defaultTypes"
        },
        {
          token: "storage.type.generic.nitro",
          regex: /#\b/
        },
        {
          token: "storage.type.regular.nitro",
          regex: /\w+\b/
        },
        {
          token: "paren.lparen.funtion_type.nitro",
          regex: /\(/,
          push: [
            {
              token: ["paren.rparen.funtion_type.nitro", "text", "punctuation.funtion_type.nitro", "text"],
              regex: /(\))(\s*)(->)(\s*)/,
              next: "pop",
            },
            {
              include: "#types",
            },
            {
              token: "punctuation",
              regex: /,/
            },
            {
              token: "text",
              regex: /\s+/
            },
            {
              token: "text",
              regex: /(?=$|\))/,
              next: "pop",
            }
          ]
        },
        {
          token: "punctuation",
          regex: /\./
        },
        {
          include: "#usage_generics"
        },
        {
          include: "#comments"
        },
      ],
      "#defaultTypes": [
        {
          token: "storage.type.buildin.nitro",
          regex: /\b(Any|Arena|Array|MemorySlice|MemoryAllocator|ArrayList|Boolean|Box|Buffer|Byte|Char|Console|Deque|Entry|Float|Fun0|Fun1|Fun2|Fun3|Fun4|Fun5|Fun6|Fun7|Fun8|Fun9|HashMap|Int|Iter|Iterator|Json|List|Map|MapEntry|Math|Memory|MemoryArena|Nothing|Null|Number|Object|Optional|Ordering|Ptr|Queue|Ref|Result|Set|Slice|Stack|String|StringMap|StringMapEntry|Pair)\b/
        },
        {
          token: "storage.type.special.nitro",
          regex: /\b(This|Self)\b/
        }
      ],
      "#struct": [
        {
          token: "keyword.nitro",
          regex: /\bstruct\b/,
          push: [
            {
              token: "punctuation.start",
              regex: /\{/,
              next: "#struct_body",
            },
            {
              token: "storage.type.nitro",
              regex: /\w+\b/,
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              include: "#definition_generics"
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /(?=$|\{|\}|=|,)/,
              next: "pop",
            },
          ],
        }
      ],
      "#struct_body": [
        {
          token: "punctuation.end",
          regex: /\}/,
          next: restart,
        },
        {
          token: ["entity.name.variable.nitro", "text", "punctuation", "text"],
          regex: /(\w+)(\s*)(:)(\s*)/,
          push: [
            {
              include: "#types",
            },
            {
              token: "text",
              regex: /(?=.)/,
              next: "pop",
            }
          ],
        },
        {
          token: "punctuation",
          regex: /,/,
        },
        {
          token: "text",
          regex: /\s+/,
        },
        {
          include: "#comments"
        },
      ],
      "#enum": [
        {
          token: "keyword.enum.nitro",
          regex: /\benum\b/,
          push: [
            {
              token: "punctuation.start",
              regex: /\{/,
              next: "#enum_body",
            },
            {
              token: "storage.type.nitro",
              regex: /\w+\b/,
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              include: "#definition_generics"
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /(?=$|\{|\}|=|,)/,
              next: "pop",
            },
          ],
        }
      ],
      '#enum_body': [
        {
          token: "punctuation.end",
          regex: /\}/,
          next: restart,
        },
        {
          token: ["entity.name.variable.nitro", "text", "punctuation", "text"],
          regex: /(\w+)(\s*)(:)(\s*)/,
          push: [
            {
              include: "#types",
            },
            {
              token: "text",
              regex: /(?=.)/,
              next: "pop",
            }
          ],
        },
        {
          token: "punctuation",
          regex: /,/,
        },
        {
          token: "text",
          regex: /\s+/,
        },
        {
          include: "#comments"
        },
      ],
      "#option": [
        {
          token: "keyword.option.nitro",
          regex: /\boption\b/,
          push: [
            {
              token: "punctuation.option.start",
              regex: /\{/,
              next: "#option_body",
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              token: "storage.type.option.nitro",
              regex: /\w+\b/,
            },
            {
              include: "#definition_generics"
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /(?=$|\{|\}|=|,)/,
              next: "pop",
            },
          ],
        }
      ],
      "#option_body": [
        {
          token: "punctuation.option.end",
          regex: /\}/,
          next: restart,
        },
        {
          token: ["storage.type.option.item.nitro", "text", "punctuation.option.item.start", "text"],
          regex: /(\w+)(\s*)(\{)(\s*)/,
          next: "#option_item_body",
        },
        {
          token: ["storage.type.option.item.nitro", "text"],
          regex: /(\w+)(\s*)/,
        },
        {
          include: "#comments"
        },
      ],
      "#option_item_body": [
        {
          token: "punctuation.option.item.end",
          regex: /\}/,
          next: restart_to("#option_body"),
        },
        {
          token: ["entity.name.variable.nitro", "text", "punctuation", "text"],
          regex: /(\w+)(\s*)(:)(\s*)/,
          push: [
            {
              include: "#types",
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /(?=$|,|}|\n)/,
              next: "pop",
            }
          ],
        },
        {
          token: "punctuation",
          regex: /,/,
        },
        {
          token: "text",
          regex: /\s+/,
        },
      ],
      "#tag": [
        {
          token: "keyword.tag.nitro",
          regex: /\btag\b/,
          push: [
            {
              token: "punctuation.tag.start",
              regex: /\{/,
              next: "#tag_body",
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              token: "storage.type.option.nitro",
              regex: /\w+\b/,
            },
            {
              include: "#definition_generics"
            },
            {
              token: "text",
              regex: /(?=$|\{|\}|=|,)/,
              next: "pop",
            },
          ],
        }
      ],
      "#tag_body": [
        {
          token: "punctuation.tag.end",
          regex: /\}/,
          next: restart,
        },
        {
          include: "#function_header"
        }
      ],
      "#function_header": [
        {
          token: "keyword.function.nitro",
          regex: /\bfun\b/,
          push: [
            {
              token: ["entity.name.function.nitro", "text", "punctuation.function_params.start", "text"],
              regex: /(\w+)(\s*)(\()(\s*)/,
              next: "#function_header_params",
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              include: "#definition_generics",
            },
            {
              token: "punctuation.namespace.nitro",
              regex: /::/,
            },
            {
              include: "#types",
            },
            {
              token: "punctuation",
              regex: /\./,
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /(?=$|\{|=)/,
              next: "pop",
            },
          ],
        }
      ],
      "#function_header_params": [
        {
          token: "punctuation.function_params.end",
          regex: /\)/,
          next: "#function_end",
        },
        {
          token: ["entity.name.variable.nitro", "text", "punctuation", "text"],
          regex: /(\w+)(\s*)(:)(\s*)/,
          push: [
            {
              include: "#types",
            },
            {
              token: "text",
              regex: /(?=$|,|\))/,
              next: "pop",
            }
          ],
        },
        {
          token: "punctuation",
          regex: /,/,
        },
        {
          token: "text",
          regex: /\s+/,
        },
        {
          token: "text.any",
          regex: /.+/,
        }
      ],
      '#function_end': [
        {
          token: "punctuation",
          regex: /:/,
        },
        {
          include: "#types",
        },
        {
          token: "text",
          regex: /(?=$|\n|\{|=)/,
          next: "pop",
        }
      ],
      "#statements": [
        {
          token: ["keyword.let.nitro", "text", "keyword.mut.nitro", "text", "entity.name.variable.nitro", "text"],
          regex: /(\blet\b)(\s*)(mut)?(\s*)(\w+)(\s*)/,
          push: [
            {
              token: "keyword.operator.assignment.nitro",
              regex: /=/,
              next: "pop",
            },
            {
              token: "text",
              regex: /(?=$|\n|\{|=)/,
              next: "pop",
            },
            {
              token: "text",
              regex: /\s+/,
            },
            {
              token: "punctuation",
              regex: /:/,
              push: [
                {
                  include: "#types"
                },
                {
                  token: "punctuation",
                  regex: /=|,|\n|$/,
                  next: "pop",
                },
              ]
            },
            {
              include: "#expressions"
            },
            {
              next: "pop",
            }
          ]
        },
        {
          token: "keyword.if.nitro",
          regex: /\bif\b/,
        },
        {
          token: "keyword.else.nitro",
          regex: /\belse\b/,
        },
        {
          token: "keyword.repeat.nitro",
          regex: /\brepeat\b/,
        },
        {
          token: "keyword.while.nitro",
          regex: /\bwhile\b/,
        },
        {
          token: "keyword.for.nitro",
          regex: /\bfor\b/,
        },
        {
          token: "keyword.defer.nitro",
          regex: /\bdefer\b/,
        },
        {
          token: "keyword.defer.nitro",
          regex: /\bdefer\b/,
        },
        {
          token: "keyword.in.nitro",
          regex: /\bin\b/,
        },
        {
          include: "#comments"
        },
        {
          defaultToken: "text.default.nitro",
          next: "pop"
        }
      ],
      "#operators": [
        {
          token: "keyword.operator.nitro",
          regex: /==|!=|===|!==|<=|>=|<|>|<=>|=>|->|::|\?/
        },
        {
          token: "keyword.operator.nitro",
          regex: /\.\.=|\.\.<|@\[|%\[|#\[|\^\^|\?\?|_|\||\#|!|&|@|\$|\^/
        },
        {
          token: "keyword.operator.nitro",
          regex: /(\+|-|\*|\/|%)(=?)/
        },
      ],
      "#keywords": [
        {
          token: "keyword.operator.assignment.nitro",
          regex: /=/
        },
        {
          token: "keyword.operator.dot.nitro",
          regex: /\./
        },
        {
          token: "keyword.operator.increment-decrement.nitro",
          regex: /\-\-|\+\+/
        },
        {
          token: "keyword.operator.arithmetic.nitro",
          regex: /[\-+*/%]/
        },
        {
          token: "keyword.operator.arithmetic.assign.nitro",
          regex: /\+=|\-=|\*=|\/=/
        },
        {
          token: "keyword.operator.logical.nitro",
          regex: /(\b(not|xor|or)\b)|!|&&|\|\|/
        },
        {
          token: "keyword.operator.range.nitro",
          regex: /\.\.[=<]/
        },
        {
          token: "punctuation",
          regex: /[;,]/
        }
      ],
      "#function_calls": [
        {
          token: ["entity.name.function.nitro", "paren.lparen"],
          regex: /(\w+\s*)(\()/,
          push: [
            {
              token: "paren.rparen",
              regex: /\)/,
              next: "pop"
            },
            {
              include: "#expressions"
            },
            {
              token: "punctuation",
              regex: /,/
            }
          ],
        }
      ],
      "#strings": [
        {
          token: "string",
          regex: /r#"/,
          push: [
            {
              token: "string",
              regex: /"#/,
              next: "pop"
            },
            {
              token: "variable.parameter.template.nitro",
              regex: /\$\w+|\${[^}]+}/
            },
            {
              token: "constant.character.escape.nitro",
              regex: /\\./
            },
            {
              defaultToken: "string"
            }
          ]
        },
        {
          token: "string",
          regex: /[au]?"/,
          push: [
            {
              token: "string",
              regex: /"/,
              next: "pop"
            },
            {
              token: "variable.parameter.template.nitro",
              regex: /\$\w+\b/,
            },
            {
              token: "variable.parameter.template.nitro",
              regex: /\$\{/,
              push: [
                {
                  token: "variable.parameter.template.nitro",
                  regex: /\}/,
                  next: "pop"
                },
                {
                  include: "#expressions"
                },
                {
                  defaultToken: "variable.parameter.template.nitro"
                }
              ]
            },
            {
              token: "constant.character.escape.nitro",
              regex: /\\./
            },
            {
              defaultToken: "string"
            }
          ]
        },
        {
          token: "string",
          regex: /```/,
          push: [
            {
              token: "string",
              regex: /```/,
              next: "pop"
            },
            {
              defaultToken: "string"
            }
          ]
        }
      ],
      "#instantiation_field": [
        {
          token: "entity.name.variable.nitro",
          regex: /\w+\b/,
        },
        {
          token: "punctuation",
          regex: /\:|,/
        },
        {
          include: "#expressions"
        },
        {
          include: "#comments"
        },
        {
          defaultToken: "text"
        }
      ],
      "#fields_definition": [
        {
          token: "punctuation",
          regex: /{/,
          push: [
            {
              token: "punctuation",
              regex: /}/,
              next: "pop",
            },
            {
              include: "#field_definition",
            },
            {
              token: "punctuation",
              regex: /,/,
            },
            {
              include: "#comments"
            },
            {
              token: "text",
              regex: /\s+/,
            }
          ]
        },
      ],
      "#field_definition": [
        {
          token: "entity.name.variable.nitro",
          regex: /\w+/,
          push: [
            {
              token: "text",
              regex: /\s+/,
            },
            {
              token: "punctuation",
              regex: /:/,
              next: "pop",
              push: [
                {
                  token: "text",
                  regex: /\s+/,
                },
                {
                  include: "#types",
                },
                {
                  token: "punctuation",
                  regex: /,|\n|$/,
                  next: "pop",
                },
              ]
            },
          ]
        },
      ],
    };

    this.normalizeRules();
  };

  NitroHighlightRules.metaData = {
    fileTypes: ["nl", "nitro"],
    name: "Nitro",
    scopeName: "source.Nitro"
  };

  oop.inherits(NitroHighlightRules, TextHighlightRules);

  exports.NitroHighlightRules = NitroHighlightRules;
});

ace.define("ace/mode/nitro", ["require", "exports", "module", "ace/lib/oop", "ace/mode/text", "ace/mode/nitro_highlight_rules", "ace/mode/folding/cstyle"], (require, exports, module) => {
  const oop = require("../lib/oop");
  const TextMode = require("./text").Mode;
  const FoldMode = require("./folding/cstyle").FoldMode;
  const NitroHighlightRules = require("./nitro_highlight_rules").NitroHighlightRules;

  const Mode = function () {
    this.HighlightRules = NitroHighlightRules;
    this.foldingRules = new FoldMode();
    this.$behaviour = this.$defaultBehaviour;
  };
  oop.inherits(Mode, TextMode);

  (function () {
    this.lineCommentStart = "//";
    this.blockComment = {start: "/*", end: "*/", nestable: true};
    this.$id = "ace/mode/nitro";
  }).call(Mode.prototype);

  exports.Mode = Mode;
});

ace.define("ace/theme/cout970", ["require", "exports", "module", "ace/lib/dom"], function (require, exports, module) {
  exports.isDark = true;
  exports.cssClass = "ace-cout970";
  exports.cssText = themeCss;
  var dom = require("../lib/dom");
  dom.importCssString(exports.cssText, exports.cssClass, false);
});

(function () {
  ace.require(["ace/theme/cout970"], function (m) {
    if (typeof module == "object" && typeof exports == "object" && module) {
      module.exports = m;
    }
  });
})();
