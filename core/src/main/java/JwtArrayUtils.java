package pdi.jwt;

public final class JwtArrayUtils {

    private JwtArrayUtils() {}

    /**
     * A constant time equals comparison - does not terminate early if
     * test will fail. For best results always pass the expected value
     * as the first parameter.
     *
     * Ported from BouncyCastle to remove the need for a runtime dependency.
     * https://github.com/bcgit/bc-java/blob/290df7b4edfc77b32d55d0a329bf15ef5b98733b/core/src/main/java/org/bouncycastle/util/Arrays.java#L136-L172
     *
     * @param expected first array
     * @param supplied second array
     * @return true if arrays equal, false otherwise.
     */
    public static boolean constantTimeAreEqual(byte[] expected, byte[] supplied)
    {
        if (expected == supplied)
        {
            return true;
        }

        if (expected == null || supplied == null)
        {
            return false;
        }

        if (expected.length != supplied.length)
        {
            return !JwtArrayUtils.constantTimeAreEqual(expected, expected);
        }

        int nonEqual = 0;

        for (int i = 0; i != expected.length; i++)
        {
            nonEqual |= (expected[i] ^ supplied[i]);
        }

        return nonEqual == 0;
    }
}
