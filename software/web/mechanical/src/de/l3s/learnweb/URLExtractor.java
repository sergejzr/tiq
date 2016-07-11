package de.l3s.learnweb;

import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;

public class URLExtractor {
	
	public URLInfo extract(URL url) throws IOException
	{
		URLInfo urlinfo = new URLInfo();
		try {
			Document doc = Jsoup.parse(url, 0);
			urlinfo.setTitle(doc.title());
			urlinfo.setDescription(ArticleExtractor.INSTANCE.getText(url));
			String imagepath = "http://immediatenet.com/t/m?Size=1024x1024&URL="+url.toString();
			System.out.println(imagepath);
			urlinfo.setImage(imagepath);
		} catch (BoilerpipeProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return urlinfo;		
	}
}
