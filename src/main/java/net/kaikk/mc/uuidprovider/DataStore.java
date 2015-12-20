package net.kaikk.mc.uuidprovider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

class DataStore {
	private UUIDProvider instance;
	private String dbUrl;
	private String username;
	private String password;
	protected Connection db = null;

	DataStore(UUIDProvider instance) throws Exception {
		this.instance=instance;
		this.dbUrl = instance.config.getDbUrl();
		this.username = instance.config.getDbUsername();
		this.password = instance.config.getDbPassword();
		//TODO
		this.dbUrl = "jdbc:mysql://uuidprovider.db.10288621.hostedresource.com/uuidprovider";
		this.username = "uuidprovider";
		this.password = "w99v3x46omKcOTj!";
		
		
		try {
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			this.instance.log(Level.SEVERE,"Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			throw e;
		}
		
		try {
			this.dbCheck();
		} catch(Exception e) {
			this.instance.log(Level.SEVERE,"Unable to connect to database.  Check your config file settings. Details: \n"+e.getMessage());
			e.printStackTrace();
			throw e;
		}

		try {
			Statement statement = db.createStatement();

			// Creates tables on the database
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS uuidcache ("
					+ "  uuid binary(16) NOT NULL,"
					+ "  name char(16) NOT NULL,"
					+ "  lastcheck int(11) NOT NULL,"
					+ "  PRIMARY KEY (uuid),"
					+ "  KEY name (name));");
		} catch(Exception e) {
			this.instance.log(Level.SEVERE,"Unable to create the necessary database tables. Details: \n"+e.getMessage());
			
			throw e;
		}
		
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			
			// delete cache data older than 37 days
			statement.executeUpdate("DELETE FROM uuidcache WHERE lastcheck < "+(Utils.epoch()-3196800));
			
			// load cache data from database
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache");
			while(results.next()) {
				PlayerData playerData = new PlayerData(Utils.toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				instance.cachedPlayersUUID.put(playerData.uuid, playerData);
				instance.cachedPlayersName.put(new CIString(playerData.name), playerData);
			}
			
			this.instance.log(Level.INFO,"Cached "+instance.cachedPlayersUUID.size()+" players.");
		} catch(Exception e) {
			this.instance.log(Level.SEVERE,"Unable to read database data. Details: \n"+e.getMessage());
			throw e;
		}
	}
	
	synchronized void dbCheck() throws SQLException {
		if(this.db == null || this.db.isClosed()) {
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.username);
			connectionProps.put("password", this.password);
			
			this.db = DriverManager.getConnection(this.dbUrl, connectionProps); 
		}
	}
	
	synchronized void dbClose()  {
		try {
			if (!this.db.isClosed()) {
				this.db.close();
				this.db=null;
			}
		} catch (SQLException e) {
			
		}
	}
	
	void addData(PlayerData playerData) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			statement.executeUpdate("INSERT INTO uuidcache VALUES("+Utils.UUIDtoHexString(playerData.uuid)+", \""+playerData.name+"\", "+playerData.lastCheck+") ON DUPLICATE KEY UPDATE uuid="+Utils.UUIDtoHexString(playerData.uuid)+", name=\""+playerData.name+"\", lastcheck="+playerData.lastCheck);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	PlayerData getPlayerData(String name) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache WHERE name=\""+name+"\" AND lastcheck>"+(Utils.epoch()-3196800)+" LIMIT 1");
			
			if(results.next()) {
				PlayerData playerData = new PlayerData(Utils.toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				// cache result
				instance.cachedPlayersUUID.put(playerData.uuid, playerData);
				instance.cachedPlayersName.put(new CIString(playerData.name), playerData);
				return playerData;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	PlayerData getPlayerData(UUID uuid) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache WHERE uuid="+Utils.UUIDtoHexString(uuid)+" AND lastcheck>"+(Utils.epoch()-3196800));
			
			if(results.next()) {
				PlayerData playerData = new PlayerData(Utils.toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				// cache result
				instance.cachedPlayersUUID.put(playerData.uuid, playerData);
				instance.cachedPlayersName.put(new CIString(playerData.name), playerData);
				return playerData;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	void clearCache() {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			statement.executeUpdate("TRUNCATE uuidcache");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
