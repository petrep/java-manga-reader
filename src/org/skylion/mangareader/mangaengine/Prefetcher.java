package org.skylion.mangareader.mangaengine;

import java.awt.BorderLayout;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.skylion.mangareader.util.StretchIconHQ;;

public class Prefetcher implements MangaEngine{

	private MangaEngine mangaEngine;
	private StretchIconHQ[] pages;
	private String[] pageURLs; 
	private String mangaName;

	private JProgressBar progressBar;//The Progress Monitor
	private JFrame parent; //The Parent Component to display the loading bar in
	private Task task;//The Swing Worker that handles repaint
	
	public Prefetcher(JFrame component, MangaEngine mangaEngine){
		this.mangaEngine = mangaEngine;
		parent = component;
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		progressBar = new JProgressBar(0, mangaEngine.getPageList().length);
		prefetch();
	}

	public void prefetch (){
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		progressBar.setValue(0);
		progressBar.setMaximum(pageURLs.length);
		progressBar.setStringPainted(true);
		pages = new StretchIconHQ[pageURLs.length];
		if(task!=null && !task.isDone()){
			System.out.println("Interrupting");
			task.cancel(true);
		}
		task = new Task();
		task.addPropertyChangeListener(new PropertyChangeListener(){
		    /**
		     * Invoked when task's progress property changes.
		     */
		    @Override
			public void propertyChange(PropertyChangeEvent evt) {
		        if ("progress" == evt.getPropertyName() ) {
		            int progress = (Integer) evt.getNewValue();
		            parent.repaint();
		            progressBar.setValue(progress);
		            progressBar.setString("Loading Page: " + progress + " of " + progressBar.getMaximum());
		        }
		    }
		});
		task.execute();
	}

	/**
	 * Returns whether or not the image could be and is in the database.
	 * Makes assumptions to speed up process.
	 * @param URL The URL you want to check
	 * @return True if it is in the database, false otherwise.
	 */
	private boolean isFetched(String URL){
		if(mangaEngine.getCurrentPageNum()>=pageURLs.length){
			System.out.println(pageURLs.length +"#"+mangaEngine.getCurrentPageNum());
			return false;
		}
		System.out.println(pageURLs[mangaEngine.getCurrentPageNum()] + "#" + URL);
		return (isCached(URL));
	}

	/**
	 * Checks solely whether or not the URL is in the database.
	 * @param URL The URL you want to check
	 * @return True if in database false otherwise.
	 */
	private boolean isCached(String URL){
		for(int i = 0; i<pageURLs.length; i++){
			if(pageURLs[i].equals(URL) && pages[i]!=null){
				System.out.println("Cached");
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param URL
	 * @return
	 */
	private StretchIconHQ fetch(String URL){
		int page = mangaEngine.getCurrentPageNum();
		if(page>=pageURLs.length){
			try {
				StretchIconHQ icon = loadImg(mangaEngine.getNextPage());
				prefetch();
				return icon;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		else {
			return pages[mangaEngine.getCurrentPageNum()];
		}
	}

	@Override
	public String getCurrentURL() {
		return mangaEngine.getCurrentURL();
	}

	@Override
	public void setCurrentURL(String url){
		mangaEngine.setCurrentURL(url);
	}

	@Override
	public StretchIconHQ loadImg(String url) throws Exception {
		if(isFetched(url)){
			mangaEngine.setCurrentURL(url);
			return fetch(url);
		}
		else{
			StretchIconHQ out =  mangaEngine.loadImg(url);
			prefetch();
			return out;
		}
	}
	
	public BufferedImage getImage(String url) throws Exception {
		return mangaEngine.getImage(url);
	}

	@Override
	public String getNextPage() {
		//Prevents the User from going to a page that hasn't been fetched yet
		String currentURL = mangaEngine.getCurrentURL();
		String nextPage = mangaEngine.getNextPage();
		if(isCached(nextPage) || task.isCancelled() || task.isDone()){
			return mangaEngine.getNextPage();
		}
		else{
			Toolkit.getDefaultToolkit().beep();
			return currentURL;
		}
	}

	@Override
	public String getPreviousPage() {
		return mangaEngine.getPreviousPage();
	}

	@Override
	public boolean isValidPage(String url) {
		return mangaEngine.isValidPage(url);
	}

	@Override
	public List<String> getMangaList() {
		return mangaEngine.getMangaList();
	}

	@Override
	public String getMangaName() {
		return mangaName;
	}

	@Override
	public String[] getChapterList() {
		return mangaEngine.getChapterList();
	}

	@Override
	public String[] getPageList() {
		return pageURLs;
	}

	@Override
	public String getMangaURL(String mangaName) {
		return mangaEngine.getMangaURL(mangaName);
	}

	@Override
	public int getCurrentPageNum() {
		return mangaEngine.getCurrentPageNum();
	}

	@Override
	public int getCurrentChapNum() {
		// TODO Auto-generated method stub
		return mangaEngine.getCurrentChapNum();
	}
	
	/**
	 * Where the actual prefetching happens
	 * @author Skylion
	 */
	class Task extends SwingWorker<Void, Void> {
		
		public Task(){
			parent.getContentPane().add(progressBar, BorderLayout.SOUTH);
			parent.revalidate();
			parent.repaint();
		}
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			try{
				for(int i = 0; i<pageURLs.length && !this.isCancelled(); i++){
					pages[i] = new StretchIconHQ(mangaEngine.getImage(pageURLs[i]));
					progressBar.setValue(i);
					progressBar.setString("Loading Page:" + (i+1) + " of " + (pageURLs.length));
				}	
			}
			catch(Exception ex){
				ex.printStackTrace();
				done();//Cleans Up 
				return null;
			}
			return null;		
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			parent.getContentPane().remove(progressBar);
			parent.revalidate();
			parent.repaint();
			
		}
		
		
	}
}

