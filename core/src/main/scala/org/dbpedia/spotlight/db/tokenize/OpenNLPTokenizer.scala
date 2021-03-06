package org.dbpedia.spotlight.db.tokenize

import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.model.{Stemmer, TokenTypeStore}
import org.dbpedia.spotlight.model._

/**
 * @author Joachim Daiber
 */

class OpenNLPTokenizer(
  tokenizer: opennlp.tools.tokenize.TokenizerME,
  stopWords: Set[String],
  stemmer: Stemmer,
  sentenceDetector: SentenceDetector,
  var posTagger: POSTaggerME,
  tokenTypeStore: TokenTypeStore
) extends BaseTextTokenizer(tokenTypeStore, stemmer) {

  def tokenize(text: Text): List[Token] = this.synchronized {
    sentenceDetector.sentPosDetect(text.text).map{ sentencePos: Span =>
      val sentence = sentencePos.getCoveredText(text.text).toString()

      val sentenceTokenPos = tokenizer.tokenizePos(sentence)
      val sentenceTokens   = Span.spansToStrings(sentenceTokenPos, sentence)
      val tokensProbs      = tokenizer.getTokenProbabilities()

      val posTags          = if(posTagger != null) posTagger.tag(sentenceTokens) else Array[String]()
      val posTagsProbs     = if(posTagger != null) posTagger.probs() else Array[Double]()

      (0 to sentenceTokens.size-1).map{ i: Int =>
        val token = if (stopWords contains sentenceTokens(i)) {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, TokenType.STOPWORD)
        } else {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, getStemmedTokenType(sentenceTokens(i)))
        }
        token.setFeature(new Score("token-prob", tokensProbs(i)))

        if(posTagger != null) {
          token.setFeature(new Feature("pos", posTags(i)))
          token.setFeature(new Score("pos-prob", posTagsProbs(i)))
        }

        if(i == sentenceTokens.size-1)
          token.setFeature(new Feature("end-of-sentence", true))

        token
      }
    }.flatten.toList
  }

  def getStringTokenizer: BaseStringTokenizer = new OpenNLPStringTokenizer(tokenizer, stemmer)

}

class OpenNLPStringTokenizer(tokenizer: opennlp.tools.tokenize.Tokenizer, stemmer: Stemmer) extends BaseStringTokenizer(stemmer) {

  def tokenizeUnstemmed(text: String): Seq[String] = this.synchronized{ tokenizer.tokenize(text) }

  def tokenizePos(text: String): Array[Span] = this.synchronized{ tokenizer.tokenizePos(text) }

  override def setThreadSafe(isThreadSafe: Boolean) {

  }
}
