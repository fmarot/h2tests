package com.teamtter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.h2.tools.DeleteDbFiles;

@Slf4j
public class H2Stuckclient {

	private static final String		DB_DRIVER			= "org.h2.Driver";
	private static final int		PORT				= 9874;
	private static final String		DB_CONNECTION		= "jdbc:h2:tcp://localhost:" + PORT + "//tmp/test;TRACE_LEVEL_FILE=4";
	int								index				= 0;
	private static final boolean	useWorkingExample	= true;	// if set to true, it demonstrate the code is working for less than 2GB files

	public static void main(String[] args) throws Exception {
		// Delete existing DB and start the new one & create the empty table
		DeleteDbFiles.execute("/tmp", "test", true);
		createDB();

		// Create a *TCP* client and watch it run endlessly. With an embeded server (non TCP) everything is fine
		H2Stuckclient client = new H2Stuckclient();
		client.insertBigFile();
		client.queryDbAndCopyFilesToOutput();

		log.info("You can find the file stored and retreived in ./target/output");
		System.exit(0);
	}

	private static void createDB() throws SQLException {
		/*server = */org.h2.tools.Server.createTcpServer(new String[] { "-tcpPort", PORT + "" }).start();
		try (Connection connection = getDBConnection()) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("CREATE TABLE FILES (OID BIGINT IDENTITY,  DATA BLOB, FILENAME VARCHAR) ");
			}
		}
	}

	void queryDbAndCopyFilesToOutput() {
		try (Connection connection = getDBConnection()) {
			File outputDir = new File("./target/output");
			outputDir.mkdirs();
			FileUtils.cleanDirectory(outputDir);

			try (Statement stmt = connection.createStatement()) {
				ResultSet rs = stmt.executeQuery("select * from FILES");
				while (rs.next()) {
					String filename = rs.getString("FILENAME");
					BigDecimal oid = rs.getBigDecimal("OID");
					log.info("OID {} - Name {}", oid, filename);
					Blob blob = rs.getBlob("DATA");
					Files.copy(blob.getBinaryStream(), new File(outputDir, filename).toPath());
				}
			}
		} catch (SQLException | IOException e) {
			log.error("", e);
		}
	}

	private void insertBigFile() {
		try (Connection connection = getDBConnection()) {
			
			File myLargeFile = null;
			String filename = null;
			if (useWorkingExample) {
				// with this little file, everything is OK in executeUpdate()
				myLargeFile = new File("./src/main/resources/littleFile");
				filename = myLargeFile.getName();
			} else {
				// with this big (2Go+) file, H2 will be stuck in executeUpdate()
				myLargeFile = new File("./src/main/resources/MAX_INT_file");
				createLargeFileIfNotExists(myLargeFile);
				filename = myLargeFile.getName();
			}

			try (PreparedStatement pstmt =
					connection.prepareStatement("INSERT INTO FILES(OID, DATA, FILENAME) "
							+ " VALUES (" + (index++) + ", ?, '" + filename + "')");
					InputStream is = new BufferedInputStream(new FileInputStream(myLargeFile))) {
				log.info("Will insert file {}", filename);
				pstmt.setBinaryStream(1, is);
				log.info(" setBinaryStream() done, will executeUpdate()", filename);
				pstmt.executeUpdate();	// H2 is stuck here for large files (or exit with connection reset, depending on the content of the file)
				log.info(" executeUpdate OK for {} ", filename);
				is.close();
			}
		} catch (IOException | SQLException e) {
			log.error("", e);
		}
	}

	private void createLargeFileIfNotExists(File myLargeFile) {
		if (!myLargeFile.exists()) {
			try (RandomAccessFile f = new RandomAccessFile(myLargeFile, "rw")) {
				f.setLength(1024 * 1024 * 1024 * 2 - 1);	// 2Go
			} catch (IOException e) {
				log.error("", e);
			}
		}
	}

	private static Connection getDBConnection() {
		Connection dbConnection = null;
		try {
			Class.forName(DB_DRIVER);
			dbConnection = DriverManager.getConnection(DB_CONNECTION);
			dbConnection.setAutoCommit(true);
		} catch (ClassNotFoundException | SQLException e) {
			log.error("", e);
		}
		return dbConnection;
	}
}
