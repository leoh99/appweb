function clearData() {
    Android.clearData();
    window.location.reload(true);
}

function initMap() {
    var map = new google.maps.Map(document.getElementById('map'), {
        center: {
            lat: -33.8688,
            lng: 151.2195
        },
        zoom: 13
    });
    google.maps.event.addListener(map, 'click', function(event) {
        var lat = event.latLng.lat();
        var lng = event.latLng.lng();
        alert("Lat=" + lat + "; Lng=" + lng);
    });
}

function travelMap() {
    var directionsDisplay = new google.maps.DirectionsRenderer;
    var directionsService = new google.maps.DirectionsService;
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 14,
        center: {
            lat: 37.77,
            lng: -122.447
        }
    });
    directionsDisplay.setMap(map);
    calculateAndDisplayRoute(directionsService, directionsDisplay);
    document.getElementById('mode').addEventListener('change', function() {
        calculateAndDisplayRoute(directionsService, directionsDisplay);
    });
}

function calculateAndDisplayRoute(directionsService, directionsDisplay) {
    var selectedMode = document.getElementById('mode').value;
    directionsService.route({
        origin: {
            lat: 37.77,
            lng: -122.447
        }, // Haight.
        destination: {
            lat: 37.768,
            lng: -122.511
        }, // Ocean Beach.
        // Note that Javascript allows us to access the constant
        // using square brackets and a string value as its
        // "property."
        travelMode: google.maps.TravelMode[selectedMode]
    }, function(response, status) {
        if (status == google.maps.DirectionsStatus.OK) {
            directionsDisplay.setDirections(response);
        } else {
            window.alert('Directions request failed due to ' + status);
        }
    });
}

function streetPanoMap() {
    // Note: constructed panorama objects have visible: true
    // set by default.
    var panorama = new google.maps.StreetViewPanorama(document.getElementById('map'), {
        position: {
            lat: 35.1682706,
            lng: 136.8968828
        },
        addressControlOptions: {
            position: google.maps.ControlPosition.BOTTOM_CENTER
        },
    });
}

function streetMap() {
    var panorama = new google.maps.StreetViewPanorama(document.getElementById('map'), {
        position: {
            lat: 35.1682706,
            lng: 136.8968828
        },
        pov: {
            heading: 165,
            pitch: 0
        },
        zoom: 1
    });
}

function transitMap() {
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 15,
        center: {
            lat: 35.1682706,
            lng: 136.8968828
        }
    });
    var transitLayer = new google.maps.TransitLayer();
    transitLayer.setMap(map);
}

function infoMap() {
    var uluru = {
        lat: -25.363,
        lng: 131.044
    };
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 4,
        center: uluru
    });
    var contentString = '<div id="content">' + '<div id="siteNotice">' + '</div>' + '<h1 id="firstHeading" class="firstHeading">Uluru</h1>' + '<div id="bodyContent">' + '<p><b>Uluru</b>, also referred to as <b>Ayers Rock</b>, is a large ' + 'sandstone rock formation in the southern part of the ' + 'Northern Territory, central Australia. It lies 335&#160;km (208&#160;mi) ' + 'south west of the nearest large town, Alice Springs; 450&#160;km ' + '(280&#160;mi) by road. Kata Tjuta and Uluru are the two major ' + 'features of the Uluru - Kata Tjuta National Park. Uluru is ' + 'sacred to the Pitjantjatjara and Yankunytjatjara, the ' + 'Aboriginal people of the area. It has many springs, waterholes, ' + 'rock caves and ancient paintings. Uluru is listed as a World ' + 'Heritage Site.</p>' + '<p>Attribution: Uluru, <a href="https://en.wikipedia.org/w/index.php?title=Uluru&oldid=297882194">' + 'https://en.wikipedia.org/w/index.php?title=Uluru</a> ' + '(last visited June 22, 2009).</p>' + '</div>' + '</div>';
    var infowindow = new google.maps.InfoWindow({
        content: contentString
    });
    var marker = new google.maps.Marker({
        position: uluru,
        map: map,
        title: 'Uluru (Ayers Rock)'
    });
    marker.addListener('click', function() {
        infowindow.open(map, marker);
    });
}

function kmlMap() {
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 11,
        center: {
            lat: 41.876,
            lng: -87.624
        }
    });
    var ctaLayer = new google.maps.KmlLayer({
        url: 'http://googlemaps.github.io/js-v2-samples/ggeoxml/cta.kml',
        map: map
    });
}

function curLocMap() {
    var map = new google.maps.Map(document.getElementById('map'), {
        center: {
            lat: -34.397,
            lng: 150.644
        },
        zoom: 15
    });
    var infoWindow = new google.maps.InfoWindow({
        map: map
    });
    // Try HTML5 geolocation.
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            var pos = {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            };
            infoWindow.setPosition(pos);
            infoWindow.setContent('Location found.');
            map.setCenter(pos);
        }, function() {
            handleLocationError(true, infoWindow, map.getCenter());
        });
    } else {
        // Browser doesn't support Geolocation
        handleLocationError(false, infoWindow, map.getCenter());
    }

}

function handleLocationError(browserHasGeolocation, infoWindow, pos) {
    infoWindow.setPosition(pos);
    infoWindow.setContent(browserHasGeolocation ? 'Error: The Geolocation service failed.' : 'Error: Your browser doesn\'t support geolocation.');
}
