var natoPhoneticAlphabet = {
    "a": "alfa",
    "b": "bravo",
    "c": "charlie",
    "d": "delta",
    "e": "echo",
    "f": "foxtrot",
    "g": "golf",
    "h": "hotel",
    "i": "india",
    "j": "juliett",
    "k": "kilo",
    "l": "lima",
    "m": "mike",
    "n": "november",
    "o": "oscar",
    "p": "papa",
    "q": "quebec",
    "r": "romeo",
    "s": "sierra",
    "t": "tango",
    "u": "uniform",
    "v": "victor",
    "w": "whiskey",
    "x": "xray",
    "y": "yankee",
    "z": "zulu",
    "1": "one",
    "2": "two",
    "3": "three",
    "4": "four",
    "5": "five",
    "6": "six",
    "7": "seven",
    "8": "eight",
    "9": "nine",
    "0": "zero",
    "/": "slash",
    "#": "hash",
    "&": "ampersand",
    "=": "equals",
    "%": "percent",
    "@": "at-sign"
};

function shuffle(text) {
    var a = text.split("");
    for(var i = a.length - 1; i > 0; i--) {
        var j = Math.floor(Math.random() * (i + 1));
        var tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
    return a.join("");
}

function generatePasswordString(num, alphabet) {
    var text = "";
    for (var i = 0; i < num; i++) {
        text += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
    }
    return text;
}

function generatePassword(numUpper, numLower, numInt, numSpecial) {
    var upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    var lower = "abcdefghijkmnopqrstuvwxyz";
    var int = "23456789";
    var special = "/#&=%@";
    return shuffle(
        generatePasswordString(numUpper, upper) +
        generatePasswordString(numLower, lower) +
        generatePasswordString(numInt, int) +
        generatePasswordString(numSpecial, special)
    );
}

function phoneticPassword(password) {
    var phonetic = "";
    for(var i=0;i<password.length;i++) {
        var p = natoPhoneticAlphabet[password.charAt(i)];
        if(p == undefined) {
             p = natoPhoneticAlphabet[password.charAt(i).toLowerCase()];
             if(p == undefined) {
                  p = password.charAt(i); // fallback
             } else {
                  p = p.toUpperCase(); 
             }
        }
        phonetic += p + " ";
    }
    return phonetic;
}
