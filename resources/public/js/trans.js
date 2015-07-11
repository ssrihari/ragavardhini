// Load the Google Transliterate API
google.load("elements", "1", {
  packages: "transliteration"
});

google.load("language", "1");

function onLoad() {
  var options = {
    sourceLanguage: google.elements.transliteration.LanguageCode.ENGLISH,
    destinationLanguage: [google.elements.transliteration.LanguageCode.HINDI],
    shortcutKey: 'ctrl+g',
    transliterationEnabled: true
  };

  // Create an instance on TransliterationControl with the required
  // options.
  var control = new google.elements.transliteration.TransliterationControl(options);

  // Enable transliteration in the textbox with id
  // 'transliterateTextarea'.
  control.makeTransliteratable(['lyrics']);
}

google.setOnLoadCallback(onLoad);

function transLyrics(line) {
  var words = line.split(" ");
  google.language.transliterate(words, "en", "hi", function(result) {
    if (result.error)
      return false;
    if (result.transliterations && result.transliterations.length > 0)
      trWords = result.transliterations.map(function(elem,i){return elem.transliteratedWords[0]});
      return trWords;
  });
}
