package models;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import play.data.validation.Required;
import static play.db.jpa.Model.*;
import play.db.jpa.Model;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.*;

/**
 * User: Loic Descotte
 * Date: 28 fevr. 2010
 */

@Entity
public class Album extends Model {

    @Required
    public String name;
    @ManyToOne
    public Artist artist;
    @Temporal(TemporalType.DATE)
    @Required
    public Date releaseDate;
    @Enumerated(EnumType.STRING)
    public Genre genre;
    public long nbVotes = 0L;
    public boolean hasCover=false;

    private static SimpleDateFormat formatYear = new SimpleDateFormat("yyyy");

    public Album(String name) {
        this.name = name;
    }

    /**
     * Set Artist
     * @param artist
     */
    public void setArtist(Artist artist){
        List<Artist> existingArtists = Artist.find("byName", artist.name).fetch();
        if(existingArtists.size()>0){
            //Artist name is unique
            this.artist=existingArtists.get(0);
        }
        else{
            this.artist=artist;
        }
    }

    /**
     * Vote for an album
     */
    public void vote(){
    	nbVotes++;
    	save();
    }

    /**
     * Find alubms by genre and year
     * @param genre
     * @param year
     * @return
     */
    public static List<Album> findByGenreAndYear(String genre, String year) {
       List<Album> albums;
       Genre genreEnum = Genre.valueOf(genre.toString().toUpperCase());
       albums = Album.find("byGenre", genreEnum).fetch();
       //LabmdaJ example
       albums = filterByYear(albums, year);
       return sortByPopularity(albums);
    }


    /**
     * Sort by popularity
     * @param albums
     * @return
     */
    private static List<Album> sortByPopularity(List<Album> albums){
    	List sortedAlbums = sort(albums, on(Album.class).nbVotes);
        //lambdaj sort is ascending
        Collections.reverse(sortedAlbums);
    	return sortedAlbums;
    }

    /**
     * LambdaJ example : Filter by year
     * @param albums
     * @param year
     * @return
     */
    public static List<Album> filterByYear(List<Album> albums, String year){
    	return select(albums, having(on(Album.class).getReleaseYear(),equalTo(year)));
    }

    /**
     * Save the album
     * @return the album
     */
    @Override
    public Album save(){
        //save artist if transient
        if(artist.id==null)
            artist.save();
        return super.save();
    }

    /**
     *
     * @param filter
     * @return found albums
     */
     public static List<Album> findAll(String filter) {
            List<Album> albums;
            if(filter != null){
                String likeFilter = "%"+filter+"%";
                //limit to 100 results
                albums = find("select a from Album a where a.name like ? or a.artist.name like ?", likeFilter, likeFilter).fetch(100);
            }
            else albums = Album.find("from Album").fetch(100);
            return sortByPopularity(albums);
	}

    /**
     *
     * @return  release year
     */
     public String getReleaseYear(){        
        return formatYear.format(releaseDate);
     }

     /**
     *
     * @return first year for recorded albums
     */
     public static int getFirstAlbumYear(){
         // get a single result via play-jpa gives the wrong result
         Date result = (Date) em().createQuery("select min(a.releaseDate) from Album a").getSingleResult();
         if(result!=null)
            return Integer.parseInt(formatYear.format(result));
         //if no album is registered return 1990
         return 1990;
     }

    /**
     *
     * @return last year for recorded albums
     */
     public static int getLastAlbumYear(){
         Date result = (Date) em().createQuery("select max(a.releaseDate) from Album a").getSingleResult();
         if(result!=null)
             return Integer.parseInt(formatYear.format(result));
         //if no album is registered return current year
         return Integer.parseInt(formatYear.format(new Date()));

     }
}
