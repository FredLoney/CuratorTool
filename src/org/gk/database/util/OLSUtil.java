package org.gk.database.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import com.jayway.jsonpath.JsonPath;

public class OLSUtil {

	private static String getRESTResponse(URI uri)
	{
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod();
		String responseBody = null;
		try {
			method.setURI(uri);
			client.executeMethod(method);
			responseBody = new String(method.getResponseBody());
		} catch (URIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseBody;
	}
	
	public static String getTermById(String termId, String ontologyName)
	{
		//Need to access their API like this, and then extract $._embedded.terms[0].description as a string and return it.
		//best: curl 'http://www.ebi.ac.uk/ols/api/ontologies/chebi/terms?obo_id=CHEBI:17794' -i -H 'Accept: application/json'
		//better, uses "short form": curl 'http://www.ebi.ac.uk/ols/api/ontologies/efo/terms?short_form=GO_0043226' -i -H 'Accept: application/json'
		//requires full IRI: curl 'http://www.ebi.ac.uk/ols/api/ontologies/efo/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FGO_0043226' -i -H 'Accept: application/json'
		String termString = "";
		try {
			URI uri = new URI("http://www.ebi.ac.uk/ols/api/ontologies/"+ontologyName+"/terms?obo_id="+termId, true);
			String responseBody = OLSUtil.getRESTResponse(uri);
			//Check that there term was found, otherwise JSONPath throws an exception.
			if (!responseBody.contains("\"status\":404"))
			{
				termString = JsonPath.read(responseBody,"$._embedded.terms[0].label");
			}
		} catch (URIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return termString;
	}
	
	public static Map<String,String> getTermXrefs(String termId, String ontologyName)
	{
		//better:  curl 'http://www.ebi.ac.uk/ols/api/search?q=CHEBI:17794&fieldList=&apos;annotation_&apos;' -i -H 'Accept: application/json'
		//extract /response/docs/./annotations_trimmed/*
		//Need to access their API like this, and then extract /annotation/_links (??) as a HashMap<String,String>
		//curl 'http://www.ebi.ac.uk/ols/api/ontologies/efo/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FGO_0043226' -i -H 'Accept: application/json'
		Map<String,String> xrefs = new HashMap<String,String>();
		List<String> xrefsList = new ArrayList<String>();
		try {
			URI uri = new URI("http://www.ebi.ac.uk/ols/api/ontologies/"+ontologyName+"/terms?obo_id="+termId, true);
			String responseBody = OLSUtil.getRESTResponse(uri);
			//Check that there term was found, otherwise JSONPath throws an exception.
			if (!responseBody.contains("\"status\":404"))
			{
				xrefsList = JsonPath.read(responseBody,"$._embedded.terms[0].annotation[?( @.database_cross_reference != null )].database_cross_reference.*");
				for (String s : xrefsList)
				{
					//The function that calls this expects a map, but is only interested in the values, not the keys, so we'll just give it
					//a map where the values are also the keys. In time, the caller and this could probably be rewritten with a simple list.
					//Or different logic to handle the map.
					//String parts[] = s.split(":");
					xrefs.put(s, s);
				}
			}
		} catch (URIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return xrefs;

	}
	
	public static Map<String,String> getTermMetadata(String termId, String ontologyName)
	{
		//JSONpath: $._embedded.terms[0].obo_synonym.[?(@.scope == "hasRelatedSynonym" || @.type == "FORMULA")]
		//Need to access their API like this, and then extract ??? as a HashMap<String,String>
		//curl 'http://www.ebi.ac.uk/ols/api/ontologies/efo/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FGO_0043226' -i -H 'Accept: application/json'
		
		Map<String,String> metadata = new HashMap<String, String>();
		
		try {
			URI uri = new URI("http://www.ebi.ac.uk/ols/api/ontologies/"+ontologyName+"/terms?obo_id="+termId, true);
			String responseBody = OLSUtil.getRESTResponse(uri);
			//Check that there term was found, otherwise JSONPath throws an exception.
			if (!responseBody.contains("\"status\":404"))
			{
				//System.out.println(responseBody);
				List<String> formulaList = JsonPath.read(responseBody,"$._embedded.terms[0][?(@.obo_synonym != null)].obo_synonym.[?(@.type=='FORMULA')].name");
				//There should only ever be one formula, but the way that the JSONPath is written, it will return a list (with only one element, hopefully).
				for (String s : formulaList)
				{
					metadata.put("FORMULA_synonym", s);
				}
				List<String> synonymList = JsonPath.read(responseBody, "$._embedded.terms[0][?(@.synonyms!=null)].synonyms[?(@ != $._embedded.terms[0].label )]");
				//The $..annotation.has_related_synonym array could also have synonyms, we should extract those too.
				List<String> relatedSynonymList = JsonPath.read(responseBody, "$._embedded.terms[0].annotation[?(@.has_related_synonym!=null)].has_related_synonym[?(@ != $._embedded.terms[0].label )]");
				
				//use a hashset to deal with possible duplicates
				synonymList.addAll(relatedSynonymList);
				Set<String> synonyms = new HashSet<String>(synonymList);
				
				int i =0;
				for (String s : synonyms)
				{
					//Some parts of the application check for the "_synonym" suffix, some parts check for a "related_synonym..." or "exact_synonym..." prefix.
					//So put BOTH in the hash. That way both methods will still work.
					metadata.put(i+"_related_synonym", s);
					metadata.put("related_synonym_"+i, s);
					metadata.put("exact_synonym_"+i, s);
					i++;
				}
				
				
				List<String> definition = JsonPath.read(responseBody, "$._embedded.terms[0][?(@.description!=null)].description.*");
				//this should only ever return a list with one item, but because the JSONPath processing thinks it *could* return more (in theory), it will return a list.
				for (String s : definition)
				{
					metadata.put("definition", s);
				}
			}
		} catch (URIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return metadata;
	}
}
