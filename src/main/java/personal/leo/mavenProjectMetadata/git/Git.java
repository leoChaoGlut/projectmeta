package personal.leo.mavenProjectMetadata.git;

import java.lang.ProcessBuilder.Redirect;

/**
 * @author leo
 * @date 2019-05-12
 */
public class Git {

    public static String clone(String gitRepoUrl) {
        String repoName = parseRepoName(gitRepoUrl);
        final String outputDirPath = "/tmp/" + repoName;
        String[] cmds = {"git", "clone", gitRepoUrl, outputDirPath};
        ProcessBuilder pb = new ProcessBuilder(cmds);
        try {
            pb.redirectError(Redirect.INHERIT);
            pb.redirectOutput(Redirect.INHERIT);
            final Process p = pb.start();
            final int existCode = p.waitFor();
            if (existCode != 0) {
                throw new RuntimeException("Git clone error:" + existCode);
            }
            return outputDirPath;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String parseRepoName(String gitRepoUrl) {
        return gitRepoUrl.substring(
            gitRepoUrl.lastIndexOf("/") + 1,
            gitRepoUrl.lastIndexOf(".")
        );
    }
}
