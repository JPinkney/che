
private static final class JavaLanguageServerSocketLauncher implements LanguageServerLauncher {

    private final LanguageServerDescription languageServerDescription;
    private final String host;
    private final int port;

    SocketLanguageServerLauncher(
            LanguageServerDescription languageServerDescription, String host, int port) {
        this.languageServerDescription = languageServerDescription;
        this.host = host;
        this.port = port;
    }

    @Override
    public LanguageServer launch(String projectPath, LanguageClient client)
            throws LanguageServerException {
        try {
            Socket socket = new Socket(host, port);
            socket.setKeepAlive(true);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            Object javaLangClient =
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class[] {LanguageClient.class, JavaLanguageClient.class},
                            new DynamicWrapper(this, client));

            Launcher<JavaLanguageServer> launcher =
                    Launcher.createLauncher(
                            javaLangClient,
                            JavaLanguageServer.class,
                            inputStream,
                            outputStream);

            launcher.startListening();
            JavaLanguageServer proxy = launcher.getRemoteProxy();
            LanguageServer wrapped =
                    (LanguageServer)
                            Proxy.newProxyInstance(
                                    getClass().getClassLoader(),
                                    new Class[] {LanguageServer.class, FileContentAccess.class},
                                    new DynamicWrapper(new JavaLSWrapper(proxy), proxy));
            return wrapped;
        } catch (IOException e) {
            throw new LanguageServerException(
                    "Can't launch language server for project: " + projectPath, e);
        }
    }

    @Override
    public LaunchingStrategy getLaunchingStrategy() {
        return PerWorkspaceLaunchingStrategy.INSTANCE;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public LanguageServerDescription getDescription() {
        return languageServerDescription;
    }

    @Override
    public boolean isAbleToLaunch() {
        return host != null && languageServerDescription != null;
    }
}