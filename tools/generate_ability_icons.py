#!/usr/bin/env python3
"""
Generates the per-ability HUD icons shown in the racial cooldown overlay.

Unlike generate_icons.py (which downscales hand-drawn per-race art), there is no
source art for individual abilities, so each icon is authored here as an explicit
16x16 pixel grid + shared palette. Output is native 16x16 RGBA PNG (Minecraft item
resolution) under textures/gui/ability/<race>/<ability>.png, keyed by the same
"<race>/<ability>" stem the Java AbilityIconRegistry derives from its resource ids.

Old generated ability icons are cleared first. Run from repo root:
    python3 tools/generate_ability_icons.py
"""
import os, glob
from PIL import Image

OUT = "src/main/resources/assets/runic_races/textures/gui/ability"

# char -> RGBA. '.' is transparent.
PALETTE = {
    '.': (0, 0, 0, 0),
    'K': (22, 20, 28, 255),     # dark outline
    'W': (245, 246, 250, 255),  # white / hot core
    'w': (198, 204, 214, 255),  # light grey
    'g': (138, 148, 160, 255),  # grey
    'd': (78, 82, 94, 255),     # dark grey
    'Y': (255, 212, 66, 255),   # bright gold
    'y': (210, 150, 42, 255),   # amber
    'o': (240, 128, 40, 255),   # orange
    'F': (255, 172, 60, 255),   # flame mid
    'f': (255, 234, 122, 255),  # flame light
    'R': (220, 62, 46, 255),    # red
    'r': (150, 32, 26, 255),    # dark red
    'm': (190, 32, 56, 255),    # crimson
    'B': (72, 122, 222, 255),   # blue
    'b': (122, 182, 240, 255),  # light blue
    'C': (92, 200, 220, 255),   # cyan
    'c': (202, 240, 250, 255),  # pale ice
    'G': (82, 190, 92, 255),    # green
    'e': (40, 120, 56, 255),    # dark green
    'n': (172, 226, 112, 255),  # lime
    'P': (150, 72, 200, 255),   # purple
    'p': (202, 152, 236, 255),  # lavender
    'v': (216, 100, 216, 255),  # magenta
    'N': (120, 80, 46, 255),    # brown
    't': (162, 122, 72, 255),   # tan / dirt
    'H': (236, 92, 122, 255),   # pink heart
    'T': (86, 210, 180, 255),   # teal
    'S': (196, 228, 238, 226),  # pale ghost (semi-transparent)
    's': (150, 182, 200, 150),  # ghost shadow (more transparent)
    'L': (250, 240, 122, 255),  # lightning yellow
    ':': (202, 152, 236, 110),  # faint fade
    '-': (190, 200, 212, 200),  # speed line
}

ICONS = {}

# ---------------- HUMAN ----------------
ICONS["primian/stroke_of_fortune"] = [  # gold four-point sparkle
    "................",
    ".......K........",
    ".......Y........",
    "......KYK.......",
    "......YfY.......",
    ".K...KYfYK...K..",
    "..KYYYYffYYYYK..",
    "...KYffffffYK...",
    "..KYYYYffYYYYK..",
    ".K...KYfYK...K..",
    "......YfY.......",
    "......KYK.......",
    ".......Y........",
    ".......K........",
    "................",
    "................",
]
ICONS["celeron/messengers_dash"] = [  # winged boot + speed lines
    "................",
    "..........WW....",
    ".........WWWW...",
    "...---...WwwW...",
    "........WWWWW...",
    "..---..WwwwW....",
    "......NNNNN.....",
    ".---.NNNNNNN....",
    ".....NtttttN....",
    ".....NtttttN....",
    "....NNtttttN....",
    "....NNNNNNNNN...",
    "....KKKKKKKKK...",
    "................",
    "................",
    "................",
]
ICONS["magi/arcane_overflow"] = [  # arcane burst
    "................",
    ".......v........",
    ".......v........",
    "...v..vPv..v....",
    "....v.vPv.v.....",
    ".....vpWpv......",
    "..vvvpWWWpvvv...",
    "...vpWWvWWpv....",
    "..vvvpWWWpvvv...",
    ".....vpWpv......",
    "....v.vPv.v.....",
    "...v..vPv..v....",
    ".......v........",
    ".......v........",
    "................",
    "................",
]
ICONS["valen/unbreakable_stand"] = [  # braced gold shield
    "................",
    "...KKKKKKKK.....",
    "..KYYYYYYYYK....",
    "..KYwwwwwwYK....",
    "..KYwYYYYwYK....",
    "..KYwYWWYwYK....",
    "..KYwYWWYwYK....",
    "..KYwYYYYwYK....",
    "..KYwwwwwwYK....",
    "...KYwwwwYK.....",
    "....KYwwYK......",
    ".....KYYK.......",
    "......KK........",
    "................",
    "................",
    "................",
]

# ---------------- ELVEN ----------------
ICONS["high_elf/arcane_reflex"] = [  # arcane diamond barrier
    "................",
    ".......W........",
    "......KpK.......",
    ".....KppvK......",
    "....KppvppK.....",
    "...KppvWvppK....",
    "..KppvWPWvppK...",
    ".KppvWPpPWvppK..",
    "..KppvWPWvppK...",
    "...KppvWvppK....",
    "....KppvppK.....",
    ".....KppvK......",
    "......KpK.......",
    ".......W........",
    "................",
    "................",
]
ICONS["dark_elf/shadowmeld"] = [  # fading hooded silhouette
    "................",
    ".....dddd.......",
    "....dKKKKd......",
    "...dKKKKKKd.....",
    "...dKKKKKKd..::.",
    "..dKKKKKKKKd.:..",
    "..dKKvvvvKKd::..",
    "..dKKvWWvKKd.:..",
    "..dKKvvvvKKd::..",
    "..dKKKKKKKKd.:..",
    "...dKKKKKKd..::.",
    "...dKKKKKKd.....",
    "....dKKKKd......",
    ".....dddd.......",
    "................",
    "................",
]
ICONS["moon_elf/moonlit_veil"] = [  # crescent moon
    "................",
    "......WWWW......",
    "....WWccccWW....",
    "...Wccccccc.....",
    "..Wcccccc.......",
    "..ccccc.........",
    ".Wcccc..........",
    ".ccccc..........",
    ".ccccc..........",
    ".Wcccc..........",
    "..ccccc.........",
    "..Wcccccc.......",
    "...Wccccccc.....",
    "....WWccccWW....",
    "......WWWW......",
    "................",
]
ICONS["blood_elf/blood_frenzy"] = [  # crimson claw marks
    "................",
    "....K..K..K.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...mR.mR.mR.....",
    "...m..m..m......",
    "................",
    "................",
    "................",
]
ICONS["ice_elf/frostbind"] = [  # snowflake
    "................",
    "........c.......",
    "..c.....c.....c.",
    "...c..c.c.c..c..",
    "....c.c.c.c.c...",
    ".....ccc.ccc....",
    "......c.c.c.....",
    "...cc..ccc..cc..",
    ".ccccccc.cccccc.",
    "...cc..ccc..cc..",
    "......c.c.c.....",
    ".....ccc.ccc....",
    "....c.c.c.c.c...",
    "...c..c.c.c..c..",
    "..c.....c.....c.",
    "................",
]

# ---------------- DWARVEN ----------------
ICONS["deep_one/tremorsense"] = [  # concentric ground ripple
    "................",
    "................",
    ".....KKKKKK.....",
    "...KK......KK...",
    "..K..gggggg..K..",
    ".K..g......g..K.",
    ".K.g..WWWW..g.K.",
    ".K.g.W....W.g.K.",
    ".K.g.W....W.g.K.",
    ".K.g..WWWW..g.K.",
    ".K..g......g..K.",
    "..K..gggggg..K..",
    "...KK......KK...",
    ".....KKKKKK.....",
    "................",
    "................",
]
ICONS["forge_one/forge_blessing"] = [  # anvil + flame
    "................",
    "........o.......",
    ".......oFo......",
    "......oFfFo.....",
    "......oFfFo.....",
    ".......ooo......",
    "................",
    "...ddddddddd....",
    "..dggggggggd....",
    "..dgggggggggd...",
    "...ddddgggdd....",
    "......dgggd.....",
    ".....ddgggdd....",
    "....dddddddd....",
    "................",
    "................",
]
ICONS["frost_one/glacial_resolve"] = [  # ice crystal cluster
    "................",
    ".......c........",
    "......cWc.......",
    "......cbc.......",
    "...c..cbc..c....",
    "..cWc.cbc.cWc...",
    "..cbc.cbc.cbc...",
    "..cbc.cbc.cbc...",
    "..cbc.cbc.cbc...",
    "..cbc.cbc.cbc...",
    "..cbbbbbbbbbbc..",
    "...cbbbbbbbbc...",
    "....cbbbbbbc....",
    ".....cccccc.....",
    "................",
    "................",
]
ICONS["iron_one/shield_wall"] = [  # metal tower shield
    "................",
    "..gggggggggg....",
    "..gddddddddg....",
    "..gdWwwwwWdg....",
    "..gdwwddwwdg....",
    "..gdwdKKdwdg....",
    "..gdwdKKdwdg....",
    "..gdwwddwwdg....",
    "..gdWwwwwWdg....",
    "..gddddddddg....",
    "...gdwwwwdg.....",
    "....gddddg......",
    ".....gddg.......",
    "......gg........",
    "................",
    "................",
]
ICONS["sky_one/mountain_leap"] = [  # up-chevrons over peak
    "................",
    ".......W........",
    "......WwW.......",
    ".....W...W......",
    "....W.....W.....",
    ".......W........",
    "......WwW.......",
    ".....W...W......",
    "....W.....W.....",
    "................",
    ".......g........",
    "......ggg.......",
    ".....ggwgg......",
    "....gggwggg.....",
    "...ggwwwggwg....",
    "..gggggggggg....",
]
ICONS["runic_one/rune_of_warding"] = [  # warding ring + glyph
    "................",
    "....YYYYYY......",
    "...Y......Y.....",
    "..Y...YY...Y....",
    ".Y...Y..Y...Y...",
    ".Y..Y....Y..Y...",
    ".Y..Y....Y..Y...",
    ".Y...YYYY...Y...",
    ".Y......Y...Y...",
    ".Y.....Y....Y...",
    "..Y...Y....Y....",
    "..Y..YYYY..Y....",
    "...Y......Y.....",
    "....YYYYYY......",
    "................",
    "................",
]

# ---------------- BESTIAL ----------------
ICONS["arachnid/web_snare"] = [  # radial spider web
    "................",
    "...w...w...w....",
    "...ww..w..ww....",
    "....w..w..w.....",
    "w....w.w.w....w.",
    ".ww...www...ww..",
    "...ww.www.ww....",
    "wwwwwww.wwwwwww.",
    "...ww.www.ww....",
    ".ww...www...ww..",
    "w....w.w.w....w.",
    "....w..w..w.....",
    "...ww..w..ww....",
    "...w...w...w....",
    "................",
    "................",
]
ICONS["avian/wind_burst"] = [  # gust swirls
    "................",
    ".....wwwww......",
    "...ww.....w.....",
    "..w........w....",
    ".ww.......w.....",
    "wwwwwwwww.w.....",
    ".........ww.....",
    "........ww......",
    "................",
    "...wwwww........",
    ".ww.....w.......",
    "w........w......",
    "wwwwwwww.w......",
    "........ww......",
    ".......ww.......",
    "................",
]
ICONS["avian/skyborne_flap"] = [  # feathered wings
    "................",
    "...K......K.....",
    "..KWK....KWK....",
    ".KWwWK..KWwWK...",
    "KWwwWK..KWwwWK..",
    "KWwwwK..KWwwwK..",
    "KWwwwWKKWwwwWK..",
    ".KWwwwWWwwwwK...",
    "..KWwwWWwwwK....",
    "...KWWWWWWK.....",
    "....KKKKKK......",
    "................",
    "................",
    "................",
    "................",
    "................",
]
ICONS["canine/howl_of_the_pack"] = [  # wolf head + howl note
    "................",
    ".g.g.......WW...",
    ".ggg......W..W..",
    "ggKggg....W.WW..",
    "gggggg.....WW...",
    "ggggggg....W....",
    "gWdgggg...WW....",
    "ggggKgg.........",
    ".gggKKg.........",
    "..ggggg.........",
    "...gggg.........",
    "....gg..........",
    "................",
    "................",
    "................",
    "................",
]
ICONS["feline/nine_lives"] = [  # cat head + "9"
    "................",
    ".Y.........Y....",
    ".YY.......YY....",
    ".YYYYYYYYYYY....",
    ".YyyyyyyyyyY....",
    ".YyKyyyyKyyY....",
    ".YyyyyyyyyyY....",
    ".YyyKKKKyyyY....",
    "..YyyyyyyyY.....",
    "...YYYYYYY......",
    "......WWW.......",
    ".....W..W.......",
    ".....WWWW.......",
    "........W.......",
    ".......W........",
    "................",
]
ICONS["feline/pounce"] = [  # diagonal gold claw streaks
    "................",
    ".........Yy.....",
    "........Yy......",
    ".......Yy..Yy...",
    "......Yy..Yy....",
    ".....Yy..Yy..Yy.",
    "....Yy..Yy..Yy..",
    "...Yy..Yy..Yy...",
    "......Yy..Yy....",
    ".....Yy..Yy.....",
    "........Yy......",
    ".......Yy.......",
    "................",
    "................",
    "................",
    "................",
]
ICONS["kitsune/foxfire_illusion"] = [  # fox head + foxfire
    "................",
    ".o.........o....",
    ".oo.......oo....",
    ".ooo.....ooo....",
    ".ooooooooooo....",
    ".oWooooooWo.....",
    ".ooKooooKoo.....",
    "..ooooooooo.....",
    "..oooWWWooo.....",
    "...ooKKKoo......",
    "....ooooo.......",
    "...C.....C......",
    "..CbC...CbC.....",
    "...C.....C......",
    "................",
    "................",
]
ICONS["serpen/shed_skin"] = [  # coiled snake S + molt flakes
    "................",
    "...GGGGG........",
    "..GG...GG.......",
    "..GG...GG.......",
    "...GGGGG........",
    "...GGGG.........",
    "....GGGG....e...",
    ".....GGGG....e..",
    "......GGGG....e.",
    ".......GGGG.....",
    "......GG.GGG....",
    ".....GG...GG....",
    ".....GG...GG....",
    "......GGGGG.....",
    "................",
    "................",
]

# ---------------- FAEBORNE ----------------
ICONS["changeling/mirror_shift"] = [  # split mirror
    "................",
    "...TTTTTTTT.....",
    "..TWcc..ccWT....",
    "..TWc....cWT....",
    "..TWc....cWT....",
    "..TWc.cc.cWT....",
    "..TWc..c.cWT....",
    "..TWc.c..cWT....",
    "..TWc.cc.cWT....",
    "..TWc....cWT....",
    "..TWc....cWT....",
    "..TWcc..ccWT....",
    "...TTTTTTTT.....",
    "................",
    "................",
    "................",
]
ICONS["dryad/verdant_bloom"] = [  # blooming flower + leaves
    "................",
    "......HH........",
    "....HHHHHH......",
    "...HHHWWHHH.....",
    "...HHWyyWHH.....",
    "..HHHWyyWHHH....",
    "...HHHWWHHH.....",
    "....HHHHHH......",
    "......HH........",
    "......eGe.......",
    ".....eeGee......",
    ".G....eGe....G..",
    ".nG...eGe...Gn..",
    "..nGGGeGeGGGn...",
    "......eGe.......",
    "................",
]
ICONS["sprite/phase_shift"] = [  # sparkle dash
    "................",
    ".......p........",
    ".......W........",
    "......pWp.......",
    ".p...pWWWp......",
    "..p.pWWWWWp.....",
    "...pWWWWWWWp....",
    "..ppWWWWWWWpp...",
    "...pWWWWWWWp....",
    "..p.pWWWWWp.....",
    ".p...pWWWp......",
    "......pWp.......",
    ".......W........",
    ".......p........",
    "................",
    "................",
]
ICONS["sprite/gossamer_wings_flap"] = [  # gossamer wings (teal)
    "................",
    "...T......T.....",
    "..TcT....TcT....",
    ".TcCcT..TcCcT...",
    "TcCCcT..TcCCcT..",
    "TcCCCT..TcCCCT..",
    "TcCCCcTTcCCCcT..",
    ".TcCCCccCCCCT...",
    "..TcCCccCCCT....",
    "...TccccccT.....",
    "....TTTTTT......",
    "................",
    "................",
    "................",
    "................",
    "................",
]
ICONS["nymph/sirens_charm"] = [  # wave + heart
    "................",
    "......HH.HH.....",
    ".....HHHHHHH....",
    ".....HHHHHHH....",
    "......HHHHH.....",
    ".......HHH......",
    "........H.......",
    "................",
    "..BB...BB...BB..",
    ".B..B.B..B.B..B.",
    "B....B....B....B",
    "................",
    "................",
    "................",
    "................",
    "................",
]
ICONS["faerie/faerie_bargain"] = [  # fae pact sigil
    "................",
    ".......T........",
    "....T..T..T.....",
    ".....TTpTT......",
    "...T.TpppT.T....",
    "....TppWppT.....",
    ".TTTpWWWWWpTTT..",
    "....TppWppT.....",
    "...T.TpppT.T....",
    ".....TTpTT......",
    "....T..T..T.....",
    ".......T........",
    "................",
    "................",
    "................",
    "................",
]
ICONS["faerie/pixie_flight_flap"] = [  # pixie wings (lavender)
    "................",
    "...P......P.....",
    "..PWP....PWP....",
    ".PWpWP..PWpWP...",
    "PWppWP..PWppWP..",
    "PWpppP..PWpppP..",
    "PWpppWPPWpppWP..",
    ".PWpppWWppppP...",
    "..PWppWWpppP....",
    "...PWWWWWWP.....",
    "....PPPPPP......",
    "................",
    "................",
    "................",
    "................",
    "................",
]

# ---------------- UNDEAD ----------------
ICONS["zombie/undying_hunger"] = [  # green toothy maw
    "................",
    "...eeeeeeeee....",
    "..eGGGGGGGGGe...",
    ".eGGGGGGGGGGGe..",
    ".eGWGWGWGWGWGe..",
    ".eGGGGGGGGGGGe..",
    ".erGGGGGGGGGre..",
    ".eGGGGGGGGGGGe..",
    ".eGWGWGWGWGWGe..",
    ".eGGGGGGGGGGGe..",
    "..eGGGGGGGGGe...",
    "...eeeeeeeee....",
    "................",
    "................",
    "................",
    "................",
]
ICONS["skeleton/conscript_the_dead"] = [  # skull + crossbones
    "................",
    ".....WWWWW......",
    "....WWWWWWW.....",
    "...WWWWWWWWW....",
    "...WWKKWKKWW....",
    "...WWKKWKKWW....",
    "...WWWWWWWWW....",
    "....WWKWKWW.....",
    ".....WWWWW......",
    "...W..WWW..W....",
    "..WWWWWWWWWWW...",
    ".W..WWWWWWW..W..",
    "W.W.......W.W...",
    ".W.........W....",
    "................",
    "................",
]
ICONS["wraith/spectral_phase"] = [  # ghost
    "................",
    "......SSSS......",
    "....SSSSSSSS....",
    "...SSSSSSSSSS...",
    "..SSSSSSSSSSSS..",
    "..SSKKSSSKKSS...",
    "..SSKKSSSKKSS...",
    "..SSSSSSSSSSSS..",
    "..SSSSSSSSSSSS..",
    "..SSSSSSSSSSSS..",
    "..SsSSsSSsSSs...",
    "..S.Ss.Ss.Ss....",
    "..s..s..s..s....",
    "................",
    "................",
    "................",
]
ICONS["demon/infernal_wrath"] = [  # horned demon face
    "................",
    ".r..........r...",
    ".rr........rr...",
    ".rrr......rrr...",
    "..rRRRRRRRRr....",
    ".rRRRRRRRRRRr...",
    ".rRRYRRRRYRRr...",
    ".rRRRRRRRRRRr...",
    ".rRRRoooooRRr...",
    "..rRRoYoYoRR....",
    "..rRRoooooRR....",
    "...rRRRRRRr.....",
    "....rRRRRr......",
    ".....rrrr.......",
    "................",
    "................",
]
ICONS["reaper/soul_harvest"] = [  # scythe + soul wisp
    "................",
    "......wwwww.....",
    "....ww.....w....",
    "...w........w...",
    "..w.........w...",
    "..w........Nw...",
    "..ww......N.....",
    "...ww....N......",
    ".....wwwN.......",
    ".......N........",
    ".......N....S...",
    "......N....SSS..",
    "......N.....S...",
    ".....N..........",
    "................",
    "................",
]

# ---------------- DRACONIC (breath cones share one silhouette, recolored) ----
ICONS["fire_drake/dragonfire_breath"] = [
    "................",
    "................",
    ".............oo.",
    "..........oFFFo.",
    ".K.......oFFFFo.",
    ".KK....oFFFFFFoo",
    ".KKK..oFFFWFFFoo",
    ".KKKKoFFFWWFFFoo",
    ".KKK..oFFFWFFFoo",
    ".KK....oFFFFFFoo",
    ".K.......oFFFFo.",
    "..........oFFFo.",
    ".............oo.",
    "................",
    "................",
    "................",
]
ICONS["ice_drake/frost_breath"] = [
    "................",
    "................",
    ".............bb.",
    "..........bcccb.",
    ".K.......bccccb.",
    ".KK....bccccccbb",
    ".KKK..bcccWcccbb",
    ".KKKKbcccWWcccbb",
    ".KKK..bcccWcccbb",
    ".KK....bccccccbb",
    ".K.......bccccb.",
    "..........bcccb.",
    ".............bb.",
    "................",
    "................",
    "................",
]
ICONS["sea_serpen/tidal_breath"] = [
    "................",
    "................",
    ".............BB.",
    "..........BbbbB.",
    ".K.......BbbbbB.",
    ".KK....BbbbbbbBB",
    ".KKK..BbbbWbbbBB",
    ".KKKKBbbbWWbbbBB",
    ".KKK..BbbbWbbbBB",
    ".KK....BbbbbbbBB",
    ".K.......BbbbbB.",
    "..........BbbbB.",
    ".............BB.",
    "................",
    "................",
    "................",
]
ICONS["terra_drake/seismic_breath"] = [
    "................",
    "................",
    ".............NN.",
    "..........NtttN.",
    ".K.......NttttN.",
    ".KK....NttttttNN",
    ".KKK..NtttWtttNN",
    ".KKKKNtttWWtttNN",
    ".KKK..NtttWtttNN",
    ".KK....NttttttNN",
    ".K.......NttttN.",
    "..........NtttN.",
    ".............NN.",
    "................",
    "................",
    "................",
]
ICONS["volt_drake/lightning_breath"] = [
    "................",
    "................",
    ".............BB.",
    "..........BLLLB.",
    ".K.......BLLLLB.",
    ".KK....BLLLLLLBB",
    ".KKK..BLLLWLLLBB",
    ".KKKKBLLLWWLLLBB",
    ".KKK..BLLLWLLLBB",
    ".KK....BLLLLLLBB",
    ".K.......BLLLLB.",
    "..........BLLLB.",
    ".............BB.",
    "................",
    "................",
    "................",
]
ICONS["wind_wyrm/galeforce_breath"] = [
    "................",
    "................",
    ".............ww.",
    "..........wcccw.",
    ".K.......wccccw.",
    ".KK....wccccccww",
    ".KKK..wcccWcccww",
    ".KKKKwcccWWcccww",
    ".KKK..wcccWcccww",
    ".KK....wccccccww",
    ".K.......wccccw.",
    "..........wcccw.",
    ".............ww.",
    "................",
    "................",
    "................",
]
ICONS["wind_wyrm/skylord_flap"] = [  # draconic wings (red-edged)
    "................",
    "...r......r.....",
    "..rWr....rWr....",
    ".rWwWr..rWwWr...",
    "rWwwWr..rWwwWr..",
    "rWwwwr..rWwwwr..",
    "rWwwwWrrWwwwWr..",
    ".rWwwwWWwwwwr...",
    "..rWwwWWwwwr....",
    "...rWWWWWWr.....",
    "....rrrrrr......",
    "................",
    "................",
    "................",
    "................",
    "................",
]


def validate():
    errors = []
    for stem, rows in ICONS.items():
        if len(rows) != 16:
            errors.append("%s: %d rows (want 16)" % (stem, len(rows)))
        for i, row in enumerate(rows):
            if len(row) != 16:
                errors.append("%s row %d: width %d (want 16)" % (stem, i, len(row)))
            for ch in row:
                if ch not in PALETTE:
                    errors.append("%s row %d: unknown char %r" % (stem, i, ch))
    return errors


def main():
    errs = validate()
    if errs:
        print("VALIDATION FAILED:")
        for e in errs:
            print("  " + e)
        raise SystemExit(1)

    # clear old generated ability icons
    for p in glob.glob(os.path.join(OUT, "*", "*.png")):
        os.remove(p)

    for stem, rows in ICONS.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                px[x, y] = PALETTE[ch]
        dest = os.path.join(OUT, stem + ".png")
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        img.save(dest)

    print("Wrote %d icons (0 missing)" % len(ICONS))


if __name__ == "__main__":
    main()
