package de.dfki.opcua.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map.Entry;

import org.opcfoundation.ua.builtintypes.DateTime;

import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.OMMSubjectTag;
import de.dfki.omm.types.TypedValue;

/**
 * A helper class for the OPC UA server to get data from the OMS.
 * 
 * @author xekl01
 *
 */
public class OmsParser {
	
	/**
	 * Fetches the names of all memories on a given OMS.
	 * 
	 * @param omsURL the URL to the Object Memory Server
	 * @return Memory names in an array of Strings
	 */
	public static String[] getOMSMemoryNames (String omsURL) {
		
		String[] result = new String[0];
		
		try {
			URL oms = new URL(omsURL+"/mgmt/memoryList");
			InputStream is = oms.openConnection().getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String memoryNames = br.readLine();
			memoryNames = memoryNames.replace("\"", "");
			memoryNames = memoryNames.substring(1, memoryNames.length()-1);
			result = memoryNames.split(",");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Memory names could not be retrieved from OMS.");
			return result;
		}
		
		return result;
	}
	
	/**
	 * Fetches the names of all memories on a given OMS.
	 * 
	 * @param omsURL the URL to the Object Memory Server
	 * @return Memory names in an ArrayList of Strings
	 */
	public static ArrayList<String> getOMSMemoryNamesList (String omsURL) {
		
		String[] memories = getOMSMemoryNames(omsURL);
		
		return new ArrayList<String>(Arrays.asList(memories));
	}
	
	/**
	 * Gets the owner's name in plain text.
	 * 
	 * @param ommURL
	 * @return owner's clear text name
	 */
	public static String getOwner(String ommURL) { 

		String owner = "";

		try {
			URL ownerURL = new URL(ommURL+"/mgmt/owner");
			InputStream is = ownerURL.openConnection().getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			owner = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Owner name could not be retrieved from OMM "+ommURL+".");
			return owner;
		}
		
		return owner;
	}
	
	/**
	 * Finds out when the block's contents were last changed.
	 * 
	 * @param block The block
	 * @return DateTime of the last change
	 */
	public static DateTime getTimeOfLastChange (OMMBlock block) {

		DateTime lastChange = null;
		
		// Get last change from Contributors
		OMMEntityCollection contributors = block.getContributors();
		if (contributors != null && !contributors.isEmpty()) {
			Calendar lastChangeCalendar = contributors.getLast().getDateAsCalendar();
			if (lastChangeCalendar != null) lastChange = new DateTime(lastChangeCalendar);
		}
		
		// If there are no contributors get it from Creator
		else {
			OMMEntity creator = block.getCreator();
			if (creator != null) {
				Calendar lastChangeCalendar = creator.getDateAsCalendar();
				if (lastChangeCalendar != null) lastChange = new DateTime(lastChangeCalendar); 
			}
		}

		return lastChange;
	}

	
	// remaining methods self explanatory
	
	public static OMMEntityCollection parseContributors (OMMBlock block) {
		
		OMMEntityCollection contributors = block.getContributors();
		if (contributors != null) return contributors;
		else return new OMMEntityCollection();

	}

	public static String parseContributorsAsString (OMMBlock block) {
		
		String contributorsString = "";
		
		OMMEntityCollection contributors = block.getContributors();
		if (contributors != null)
			for (OMMEntity entity : contributors) {
				contributorsString += entity.toString() + "\n";
			}

		return contributorsString;
	}

	public static String parseCreator(OMMBlock block) {

		String creatorString = "";
		
		OMMEntity creator = block.getCreator();
		if (creator != null) {
			creatorString = creator.toString();
		}

		return creatorString;
	}

	public static String parseDescription(OMMBlock block) {

		String descriptionString = "";
		
		OMMMultiLangText descriptions = block.getDescription();
		if (descriptions != null)
			for (Entry<Locale, String> description : descriptions.entrySet()) {
				descriptionString += description.getKey().getLanguage() + ": " + description.getValue() +"\n";
			}

		return descriptionString;
		
	}

	public static String parseFormat(OMMBlock block) {

		String formatString = "";
		
		OMMFormat format = block.getFormat();
		if (format != null) {
			formatString = format.toString();
		}

		return formatString;
		
	}

	public static String parseId(OMMBlock block) {

		return block.getID();
		
	}

	public static String parseLink(OMMBlock block) {

		String linkString = "";
		
		if (block.isLinkBlock()) {
			TypedValue link = block.getLink();
			if (link != null) linkString = link.getValue().toString();
		}
		
		return linkString;

	}
	
	public static String parseNamespace(OMMBlock block) {

		String namespaceString = "";
		
		URI namespace = block.getNamespace();
		if (namespace != null) {
			namespaceString = namespace.toString();
		}

		return namespaceString;

	}
	
	public static String parsePayload(OMMBlock block) {

		String payloadString = "";
		
		try {
			payloadString = block.getPayloadAsString();
		}
		catch (Exception e) {
			System.err.println("No payload found for block "+block.getID()+", representing it as empty.");
		}

		return payloadString;

	}

	public static String parsePrimaryID(OMMBlock block) {

		String primaryIdString = "";
		
		TypedValue primaryId = block.getPrimaryID();
		if (primaryId != null) {
			primaryIdString = primaryId.getValue().toString();
		}

		return primaryIdString;
		
	}

	public static String parseSubject(OMMBlock block) {

		String subjectString = "";
		
		OMMSubjectCollection subjects = block.getSubject();
		if (subjects != null)
			for (OMMSubjectTag subject : subjects) {
				subjectString += subject.toString()+"\n";
			}

		return subjectString;

	}

	public static String parseTitle(OMMBlock block) {

		String titleString = "";
		
		OMMMultiLangText titles = block.getTitle();
		if (titles != null)
			for (Entry<Locale, String> title : titles.entrySet()) {
				titleString += title.getKey().getLanguage() + ": " + title.getValue() +"\n";
			}

		return titleString;

	}

	public static String parseType(OMMBlock block) {

		String typeString = "";
		
		URL type = block.getType();
		if (type != null) {
			typeString = type.toString();
		}

		return typeString;
		
	}

}
