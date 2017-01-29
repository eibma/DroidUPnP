package org.droidupnp.model.cling;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.PlaylistItem;
import org.fourthline.cling.support.model.item.TextItem;
import org.fourthline.cling.support.model.item.VideoItem;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;
import android.util.Xml;

public class TrackMetadata {

    protected static final String TAG = "TrackMetadata";

    public String id;
    public String title;
    public String artist;
    public String genre;
    public URI artURI;
    public String res;
    public String itemClass;

    @Override
    public String toString() {
        return "TrackMetadata [id=" + id + ", title=" + title + ", artist=" + artist + ", genre=" + genre + ", artURI="
                + artURI + "res=" + res + ", itemClass=" + itemClass + "]";
    }

    public TrackMetadata(String xml) {
        parseTrackMetadata(xml);
    }

    public TrackMetadata(Item UPNPItem) {
        String type = "";
        if (UPNPItem instanceof AudioItem)
            type = "audioItem";
        else if (UPNPItem instanceof VideoItem)
            type = "videoItem";
        else if (UPNPItem instanceof ImageItem)
            type = "imageItem";
        else if (UPNPItem instanceof PlaylistItem)
            type = "playlistItem";
        else if (UPNPItem instanceof TextItem)
            type = "textItem";

        this.id = UPNPItem.getId();
        this.title = UPNPItem.getTitle();
        this.artist = getArtistOfItem(UPNPItem) + "wtf";
        this.genre = ""; // todo;
        this.artURI = getAlbumArtURIOfItem(UPNPItem);
        this.res = UPNPItem.getFirstResource().getValue();
        this.itemClass = "object.item." + type;
    }

    /**
     * Determine the artist, or something that should be displayed as an artist of a given UPNPItem.
     * Current solution does not cover multiple artists, compositors or original artists
     *
     * @param UPNPItem item to get the artist from
     * @return name of the artist
     */
    private String getArtistOfItem(Item UPNPItem) {
        if (UPNPItem != null) {
            MusicTrack musicTrack = ((MusicTrack) UPNPItem);
            if (musicTrack.getArtists() != null && musicTrack.getArtists().length > 0) {
                //todo: check alternatives
                return musicTrack.getArtists()[0].getName();
            } else {
                Log.e(TAG, "Could not determine artist of: " + musicTrack.toString());
            }
        }

        return "";
    }

    /**
     * @param UPNPItem
     * @return
     */
    private URI getAlbumArtURIOfItem(Item UPNPItem) {
        URI uri = null;
        if (UPNPItem.getProperties() != null && !UPNPItem.getProperties().isEmpty()) {
            for (DIDLObject.Property property : UPNPItem.getProperties()) {
                if ("albumArtURI".equals(property.getDescriptorName())) {
                    uri = (URI) property.getValue();
                    break;
                }
            }
        }
        return uri;
    }

    private XMLReader initializeReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader xmlreader = parser.getXMLReader();
        return xmlreader;
    }

    public void parseTrackMetadata(String xml) {
        if (xml == null)
            return;

        Log.d(TAG, "XML : " + xml);

        try {
            XMLReader xmlreader = initializeReader();
            UpnpItemHandler upnpItemHandler = new UpnpItemHandler();

            xmlreader.setContentHandler(upnpItemHandler);
            xmlreader.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            // e.printStackTrace();
            Log.w(TAG, "Error while parsing metadata !");
            Log.w(TAG, "XML : " + xml);
        }
    }

    public String getXML() {
        XmlSerializer s = Xml.newSerializer();
        StringWriter sw = new StringWriter();

        try {
            s.setOutput(sw);

            s.startDocument(null, null);
            s.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            //start a tag called "root"
            s.startTag(null, "DIDL-Lite");
            s.attribute(null, "xmlns", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
            s.attribute(null, "xmlns:dc", "http://purl.org/dc/elements/1.1/");
            s.attribute(null, "xmlns:upnp", "urn:schemas-upnp-org:metadata-1-0/upnp/");
            s.attribute(null, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");

            s.startTag(null, "item");
            s.attribute(null, "id", "" + id);
            s.attribute(null, "parentID", "");
            s.attribute(null, "restricted", "1");

            if (title != null) {
                s.startTag(null, "dc:title");
                s.text(title);
                s.endTag(null, "dc:title");
            }

            if (artist != null) {
                s.startTag(null, "dc:creator");
                s.text(artist);
                s.endTag(null, "dc:creator");
            }

            if (genre != null) {
                s.startTag(null, "upnp:genre");
                s.text(genre);
                s.endTag(null, "upnp:genre");
            }

            if (artURI != null) {
                s.startTag(null, "upnp:albumArtURI");
                s.attribute(null, "dlna:profileID", "JPEG_TN");
                s.text(artURI.toString());
                s.endTag(null, "upnp:albumArtURI");
            }

            if (res != null) {
                s.startTag(null, "res");
                s.text(res);
                s.endTag(null, "res");
            }

            if (itemClass != null) {
                s.startTag(null, "upnp:class");
                s.text(itemClass);
                s.endTag(null, "upnp:class");
            }

            s.endTag(null, "item");

            s.endTag(null, "DIDL-Lite");

            s.endDocument();
            s.flush();

        } catch (Exception e) {
            Log.e(TAG, "error occurred while creating xml file : " + e.toString());
            e.printStackTrace();
        }

        String xml = sw.toString();
        Log.d(TAG, "TrackMetadata : " + xml);

        return xml;
    }

    public class UpnpItemHandler extends DefaultHandler {

        private final StringBuffer buffer = new StringBuffer();

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            buffer.setLength(0);
            // Log.v(TAG, "startElement, localName="+ localName + ", qName=" + qName);

            if (localName.equals("item")) {
                id = atts.getValue("id");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("title")) {
                title = buffer.toString();
            } else if (localName.equals("creator")) {
                artist = buffer.toString();
            } else if (localName.equals("genre")) {
                genre = buffer.toString();
            } else if (localName.equals("albumArtURI")) {
                try {
                    artURI = new URI(buffer.toString());
                } catch (Exception e) {

                }
            } else if (localName.equals("class")) {
                itemClass = buffer.toString();
            } else if (localName.equals("res")) {
                res = buffer.toString();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            buffer.append(ch, start, length);
        }
    }
}