class Web {

	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				System.out.println("Usage: java Web #port");
				return;
			}

			int port = Integer.parseInt(args[0]);
			HttpServer srv = new HttpServer();
			srv.initWebServer(port);
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

}
