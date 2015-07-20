package com.teamtter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.tools.DeleteDbFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H2FileDatabaseExample {

	private static final String DB_DRIVER = "org.h2.Driver";
	private static final String DB_CONNECTION = "jdbc:h2:~/test";
	private static final String DB_USER = "";
	private static final String DB_PASSWORD = "";

	public static void main(String[] args) throws Exception {
		try {
			// delete the H2 database named 'test' in the user home directory
			DeleteDbFiles.execute("~", "test", true);
			insertWithStatement();

		} catch (SQLException e) {
			log.error("" ,e);
		}
	}


	// H2 SQL Statement Example
	private static void insertWithStatement() throws SQLException {
		Statement stmt = null;
		try (Connection connection = getDBConnection()) {
			connection.setAutoCommit(true);
			stmt = connection.createStatement();
			stmt.execute("CREATE TABLE PERSON(id int primary key, name varchar(255))");
			stmt.execute("INSERT INTO PERSON(id, name) VALUES(1, 'Anju')");
			stmt.execute("INSERT INTO PERSON(id, name) VALUES(2, 'Sonia')");
			stmt.execute("INSERT INTO PERSON(id, name) VALUES(3, 'Asha')");

			ResultSet rs = stmt.executeQuery("select * from PERSON");
			System.out.println("H2 Database inserted through Statement");
			while (rs.next()) {
				log.info("Id {} - Name {}", rs.getInt("id"), rs.getString("name"));
			}
			stmt.close();
//			connection.commit();
		} catch (SQLException e) {
			log.error("" ,e);
		}
	}

	private static Connection getDBConnection() {
		Connection dbConnection = null;
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			log.error("" ,e);
		}
		try {
			dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
			return dbConnection;
		} catch (SQLException e) {
			log.error("" ,e);
		}
		return dbConnection;
	}
}