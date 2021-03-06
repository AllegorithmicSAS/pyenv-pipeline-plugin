package com.github.pyenvpipeline.jenkins;

import hudson.FilePath;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ManagedVirtualenv extends AbstractVirtualenv {

    private static final Logger LOGGER = Logger.getLogger(ManagedVirtualenv.class.getCanonicalName());

    public ManagedVirtualenv(String withPythonEnvBlockArgument, boolean isUnix) {
        super(withPythonEnvBlockArgument, isUnix);
    }

    // This represents an externally managed virtualenv. We are not allowed to touch it
    @Override
    public boolean canCreate() {
        return false;
    }

    // No value can be returned here that makes sense. In any event, this method should never be called anyways
    @Override
    public String getPythonInstallationPath() {
        return null;
    }

    @Override
    public String getVirtualEnvPath() {
        return withPythonEnvBlockArgument;
    }

    public static class Factory extends AbstractVirtualenvFactory<ManagedVirtualenv> {

        List<String> windowsVirtualenvContentPaths = Arrays.asList(
           "Scripts\\activate.bat", "Scripts\\deactivate.bat"
        );

        List<String> unixVirtualenvContentPath = Arrays.asList(
            "bin/activate", "bin/python"
        );

        @Override
        public boolean canBeBuilt(String withPythonEnvArgument, StepContextWrapper stepContextWrapper) throws Exception {
            // Here, we need to determine if withPythonEnvArgument represents a path. For now, we will
            // use this super naive method

            boolean directoryTest = (stepContextWrapper.isUnix() && withPythonEnvArgument.endsWith("/")) || (!stepContextWrapper.isUnix() && withPythonEnvArgument.endsWith("\\"));
            return directoryTest && verifyExistenceOfFiles(withPythonEnvArgument, stepContextWrapper, stepContextWrapper.isUnix() ? unixVirtualenvContentPath : windowsVirtualenvContentPaths);
        }

        @Override
        public ManagedVirtualenv build(String withPythonEnvBlockArgument, StepContextWrapper stepContextWrapper) {
            return new ManagedVirtualenv(withPythonEnvBlockArgument, stepContextWrapper.isUnix());
        }

        private boolean verifyExistenceOfFiles(String withPythonEnvArgument, StepContextWrapper stepContextWrapper, List<String> relativeFiles) throws Exception {
            FilePath virtualenvDirectory = stepContextWrapper.getStepContext().get(FilePath.class).child(withPythonEnvArgument);

            boolean verified = true;

            for (String relativeFilePath : relativeFiles) {
                FilePath relative = virtualenvDirectory.child(relativeFilePath);
                LOGGER.info("Checking for the existence of: " + relative.getRemote());

                verified = relative.exists();

                if (!verified) {
                    LOGGER.info(relative.getRemote() + " not found");
                    break;
                }
            }

            return verified;
        }
    }
}
