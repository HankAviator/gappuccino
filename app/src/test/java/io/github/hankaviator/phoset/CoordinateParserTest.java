package io.github.hankaviator.phoset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class CoordinateParserTest {
    @Test
    public void parsesObservedPhotosUri() {
        assertGeo("geo:30.301739,120.101775",
                CoordinateParser.parseGeoUri("geo:?z=16&q=30.301739%2C120.101775"));
    }

    @Test
    public void parsesStandardGeoUri() {
        assertGeo("geo:-33.8688,151.2093",
                CoordinateParser.parseGeoUri("geo:-33.868800,151.209300?z=16"));
    }

    @Test
    public void parsesGoogleMapsCoordinateUrls() {
        assertGeo("geo:30.301739,120.101775", CoordinateParser.parseGoogleMapsUri(
                "https://www.google.com/maps/search/?api=1&query=30.301739%2C120.101775"));
        assertGeo("geo:51.5,-0.12", CoordinateParser.parseGoogleMapsUri(
                "https://maps.google.co.uk/maps?q=51.5%2C-0.12"));
    }

    @Test
    public void rejectsAddressesAndPlaceSearches() {
        assertNull(CoordinateParser.parseGeoUri("geo:0,0?q=West+Lake%2C+Hangzhou"));
        assertNull(CoordinateParser.parseGeoUri("geo:?q=30.301739%2C120.101775+(photo)"));
        assertNull(CoordinateParser.parseGoogleMapsUri(
                "https://www.google.com/maps/search/?api=1&query=West+Lake"));
    }

    @Test
    public void rejectsRoutesAndNonGoogleUrls() {
        assertNull(CoordinateParser.parseGoogleMapsUri(
                "https://www.google.com/maps/dir/?api=1&destination=30.3%2C120.1"));
        assertNull(CoordinateParser.parseGoogleMapsUri(
                "https://example.com/maps?q=30.3%2C120.1"));
    }

    @Test
    public void validatesCoordinateRangesAndSyntax() {
        assertGeo("geo:90,-180", CoordinateParser.parsePair("+90.0, -180.000"));
        assertNull(CoordinateParser.parsePair("90.0001,0"));
        assertNull(CoordinateParser.parsePair("0,180.0001"));
        assertNull(CoordinateParser.parsePair("NaN,120"));
        assertNull(CoordinateParser.parsePair("30.3,120.1 extra"));
        assertNull(CoordinateParser.parsePair("30.3"));
    }

    @Test
    public void createsPinnedGeoUri() {
        Coordinate coordinate = CoordinateParser.parsePair("30.301739,120.101775");
        assertEquals("geo:30.301739,120.101775?q=30.301739,120.101775",
                coordinate.toPinnedGeoUri());
    }

    @Test
    public void opensAmapPlaceDetailsWithGpsCoordinates() {
        Coordinate coordinate = CoordinateParser.parsePair("30.250000,120.150000");
        assertEquals("androidamap://viewReGeo?sourceApplication=GooglePhotos"
                        + "&lat=30.25&lon=120.15&dev=1",
                coordinate.toAmapPlaceUri());
    }

    @Test
    public void preservesSignedCoordinatesForAmap() {
        Coordinate coordinate = CoordinateParser.parsePair("-33.868800,151.209300");
        assertEquals("androidamap://viewReGeo?sourceApplication=GooglePhotos"
                        + "&lat=-33.8688&lon=151.2093&dev=1",
                coordinate.toAmapPlaceUri());
    }

    private static void assertGeo(String expected, Coordinate coordinate) {
        assertEquals(expected, coordinate.toGeoUri());
    }
}
