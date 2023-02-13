package pdi.jwt

object JwtArrayUtils {

  /** A constant time equals comparison - does not terminate early if test will fail. For best
    * results always pass the expected value as the first parameter.
    *
    * Ported from BouncyCastle to remove the need for a runtime dependency.
    * https://github.com/bcgit/bc-java/blob/290df7b4edfc77b32d55d0a329bf15ef5b98733b/core/src/main/java/org/bouncycastle/util/Arrays.java#L136-L172
    *
    * @param expected
    *   first array
    * @param supplied
    *   second array
    * @return
    *   true if arrays equal, false otherwise.
    */
  def constantTimeAreEqual(expected: Array[Byte], supplied: Array[Byte]): Boolean =
    if (expected == supplied) true
    else if (expected == null || supplied == null) false
    else if (expected.length != supplied.length)
      !JwtArrayUtils.constantTimeAreEqual(expected, expected)
    else {
      var nonEqual = 0
      (0 until expected.length).foreach(i => nonEqual |= (expected(i) ^ supplied(i)))
      nonEqual == 0
    }
}
