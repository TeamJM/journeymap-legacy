"use strict";
/**
 * JourneyMap web client - http://journeymap.info
 * Copyright (C) 2011-2015. Mark Woodman (journeymap.info) All rights reserved.
 * May not be modified or distributed without express written consent.
 */
var JourneyMap = (function() {
	var map;
	var mapOverlay;

	var isNightMap = false;
	var centerOnPlayer = true;
	var showCaves = true;	
	var showAnimals = true;
	var showPets = true;
	var showMobs = true;
	var showVillagers = true;
	var showPlayers = true;
	var showWaypoints = true;
	var showGrid = true;

	var lastImageCheck = 0;
	var skipImageCheck = false;
	
	var JmIcon;
	var halted = false;
	var uiInitialized = false;
	var versionChecked = false;
	var updatingMap = false;
	var drawingMap = false;

	var playerOverrideMap = false;
	var playerUnderground = false;

	var queryServerPending = 0;
	var timerId = null;

	var JM = {
		debug : false,
		messages : null,
		properties : null,
		mobs : null,
		animals : null,
		players : null,
		villagers : null,
		waypoints : null,
		images : null
	};
	var markers = {};
	
	var entityTemplate = [
	    '<div class="entityMarker" id="">',
	    '<div class="entityName"/>',
	    '<div class="entityImages">',			    
	    '<img class="entityLocator" src="/img/pixel.png">',
	    '<img class="entityIcon" src="/img/pixel.png" >',
	    '</div>',
	    '</div>'
	].join('');
	
	var mpTemplate = [
	    '<div class="mpMarker" id="">',
	    '<div class="mpName"/>',
	    '<div class="mpImages">',			    
	    '<img class="mpLocator" src="/img/locator-other.png">',
	    '<img class="mpIcon" src="/img/pixel.png" >',
	    '</div>',
	    '</div>'
	].join('');	          
	
	var dialogTemplate = [
		'<div class="dialog">',
		'<img src="/img/ico/journeymap144.png">',
		'<div></div>',
		'</div>'
	].join('');	
	
	var messageTemplate = '<div class="message"></div>';
	
	
	var errorDialog = null;
	var splashDialog = null;
	var debug = false;
	
	// Preload images
	$('<img>').attr('src','/img/pixel.png');

	/** Delay helper * */
	var delay = (function() {

		var timer = 0;
		return function(callback, ms) {

			clearTimeout(timer);
			timer = setTimeout(callback, ms);
		};
	})();

	/**
	 * Load I18N messages once.
	 * 
	 * @returns
	 */
	var initMessages = function() {

		if (debug)
			console.log(">>> " + "initMessages");

		$.ajax({
			
			url : "/properties",
			dataType : "jsonp",
			contentType : "application/javascript; charset=utf-8",
			async : false
			
		}).fail(handleError).done(function(data, textStatus, jqXHR) {
			JM.properties = data;
			
			// Set global vars of prefs
			showCaves = JM.properties.showCaves.value==1;
            showGrid = JM.properties.showGrid.value==1;
            showMobs = JM.properties.showMobs.value==1;
			showAnimals = JM.properties.showAnimals.value==1;
			showVillagers = JM.properties.showVillagers.value==1;
			showPets = JM.properties.showPets.value==1;
			showPlayers = JM.properties.showPlayers.value==1;
			showWaypoints = JM.properties.showWaypoints.value==1;
			
			// Get L10N messages
			$.ajax({
				url : "/data/messages",
				dataType : "jsonp",
				contentType : "application/javascript; charset=utf-8",
				async : false
			}).fail(handleError).done(function(data, textStatus, jqXHR) {
				JM.messages = data;
				initGame();
			});
			
		});
				
	}

	/**
	 * Load Game info once.
	 * 
	 * @returns
	 */
	var initGame = function() {

		if (debug)
			console.log(">>> " + "initGame");

		$.ajax({
			url : "/data/world",
			dataType : "jsonp",
			contentType : "application/javascript; charset=utf-8",
			async : false
		}).fail(handleError).done(
			function(data, textStatus, jqXHR) {

				JM.world = data;
				_gaq.push(['_setCustomVar', 1, 'jm_version', JM.world.jm_version, 2]);
				_gaq.push(['_setCustomVar', 2, 'mc_version', JM.world.mc_version, 2]);

				// Update UI with game info
				$("#version").html("JourneyMap " + JM.world.jm_version);
				
				// Splash
				if(!errorDialog) {
					splashDialog = createDialog(JM.world.mod_name + "<br><small>by techbrew</small>");
				}

				// Init UI
				initUI();
			}
		);
	}

	/**
	 * Initialize UI once.
	 * 
	 * @returns
	 */
	var initUI = function() {

		if (debug)
			console.log(">>> " + "initUI");

		// Ensure messages are loaded first.
		if (!JM.messages) {
			throw ("initUI called without JM.messages"); // shouldn't happen
		}

		// Set page language
		$('html').attr('lang', JM.messages.locale.split('_')[0]);

		// Set RSS feed title
		$("link #rssfeed").attr("title", getMessage('rss_feed_title'));		

		// Main menu	
		$("#jm-menu").menu().hide().find('a').click(function(){
			_gaq.push(['_trackEvent', 'menu', 'link', this.href]);
			return true;
		});
		
		$("#jm-button").button({
			icons: {
				secondary: "ui-icon-triangle-1-s"
	        }
		}).click(function(){
			var menu = $("#jm-menu");
			if ( $(menu).is(':visible') ) {
				menu.hide();
			} else {
				var button = $("#jm-button");
				menu.show().position({ my: "left top", at: "left bottom", of: button });
				$( document ).one( "click", function() { menu.hide(); });
			}
	        return false;
		});
		
		// JourneyMap menu / homepage link
		$("#webLink").attr("title", getMessage('web_link_title'));
		$("#webLinkText").html(getMessage('web_link_text'));
		
		// JourneyMap menu / forums link
		$("#forumLink").attr("title", getMessage('forum_link_title'));
		$("#forumLinkText").html(getMessage('forum_link_text'));
		
		// JourneyMap menu / RSS feed link
		$("#rssLink").attr("title", getMessage('rss_feed_title'));
		$("#rssLinkText").html(getMessage('rss_feed_text'));

		// JourneyMap menu / Email subscription link
		$("#emailLink").attr("title", getMessage('email_sub_title'));
		$("#emailLinkText").html(getMessage('email_sub_text'));

		// JourneyMap menu / Follow on Twitter link
		$("#twitterLink").attr("title", getMessage('follow_twitter_title'));
		$("#twitterLinkText").html(getMessage('follow_twitter_text'));
		
		// JourneyMap menu / Donate link
		$("#donateLink").attr("title", getMessage('donate_title'));
		$("#donateLinkText").html(getMessage('donate_text'));
		
		// Toggles / Day/Night button
		$("#dayNightButton")
			.attr("title", getMessage('day_button_title'))
			.click(function() {
				playerOverrideMap = true;
				if(isNightMap===true) {
					setMapType('day');
				} else {								
					setMapType('night');
				}
			});

		$("#dayNightButton").parent().buttonset();
		
		// Toggles / Follow button
		$("#followButton")
			.attr("title", getMessage('follow_button_title'))
			.click(function() {
				setCenterOnPlayer(!centerOnPlayer);
			});
		$("#jm-toggles").buttonset();
		
		// Options Menu and Button		
		$("#jm-options-menu").menu().hide();
		$("#jm-options-button").attr("title", getMessage('show_menu_text'));
		$("#jm-options-button").button({
			icons: {
				secondary: "ui-icon-triangle-1-s"
	        }
		}).click(function(){
			$("#jm-actions-menu").hide();
			var menu = $("#jm-options-menu");
			if ( $(menu).is(':visible') ) {
				menu.hide();
			} else {		

				enforceFeature("#checkShowCaves", JM.world.features.MapCaves);
				enforceFeature("#checkShowAnimals", JM.world.features.RadarAnimals);
				enforceFeature("#checkShowPets", JM.world.features.RadarAnimals);
				enforceFeature("#checkShowMobs", JM.world.features.RadarMobs);
				enforceFeature("#checkShowVillagers", JM.world.features.RadarVillagers);
				enforceFeature("#checkShowPlayers", JM.world.features.RadarPlayers);
				
				menu.show();
				$( document ).one( "click", function() { menu.hide(); });
			}			
	        return false;
		});
		
		$("#jm-options-menu").click( function(event){
			event.stopPropagation();
		});
		
		
		// Options Menu items
		setTextAndTitle("#cavesMenuItem", "caves_menu_item_text", "caves_menu_item_title");
		setTextAndTitle("#animalsMenuItem", "animals_menu_item_text", "animals_menu_item_title");
		setTextAndTitle("#petsMenuItem", "pets_menu_item_text", "pets_menu_item_title");
		setTextAndTitle("#mobsMenuItem", "mobs_menu_item_text", "mobs_menu_item_title");
		setTextAndTitle("#villagersMenuItem", "villagers_menu_item_text", "villagers_menu_item_title");
		setTextAndTitle("#playersMenuItem", "players_menu_item_text", "players_menu_item_title");
		setTextAndTitle("#waypointsMenuItem", "waypoints_menu_item_text", "waypoints_menu_item_title");
		setTextAndTitle("#gridMenuItem", "grid_menu_item_text", "grid_menu_item_title");
		
		// Options Menu checkboxes
		$("#checkShowCaves").prop('checked', showCaves)		
		$("#checkShowCaves").click(function(event) {
			showCaves = (this.checked === true);
			postPreference("showCaves", showCaves);
			if(playerUnderground) {
				refreshMap();			
			}
		});
		
		$("#checkShowGrid").prop('checked', showGrid)		
		$("#checkShowGrid").click(function(event) {
			showGrid = (this.checked === true);
			postPreference("showGrid", showGrid);
			setTimeout(function() {
                window.location = window.location;
            }, 1500);
		});
		
		$("#checkShowWaypoints").prop('checked', showWaypoints)		
		$("#checkShowWaypoints").click(function(event) {
			showWaypoints = (this.checked === true);
			postPreference("showWaypoints", showWaypoints);
			if(!showWaypoints) {
				JM.waypoints = null;				
			}
		});
		
		$("#checkShowAnimals").prop('checked', showAnimals)
		$("#checkShowAnimals").click(function(event) {
			showAnimals = (this.checked === true);
			postPreference("showAnimals", showAnimals);
			drawMobs();
		});

		$("#checkShowPets").prop('checked', showPets)
		$("#checkShowPets").click(function(event) {
			showPets = (this.checked === true);
			postPreference("showPets", showPets);
			drawMobs();
		});

		$("#checkShowMobs").prop('checked', showMobs)
		$("#checkShowMobs").click(function() {
			showMobs = (this.checked === true);
			postPreference("showMobs", showMobs);
			drawMobs();
		});

		$("#checkShowVillagers").prop('checked', showVillagers)
		$("#checkShowVillagers").click(function() {
			showVillagers = (this.checked === true);
			postPreference("showVillagers", showVillagers);
			drawMobs();
		});

		$("#checkShowPlayers").prop('checked', showPlayers)
		$("#checkShowPlayers").click(function() {
			showPlayers = (this.checked === true);
			postPreference("showPlayers", showPlayers);
			drawMultiplayers();
		});
		
		// Actions Menu Button
		$("#jm-actions-menu").menu().hide();
		$("#jm-actions-button").attr("title", getMessage('actions_title'));
		$("#jm-actions-button").button({
			icons: {
				secondary: "ui-icon-triangle-1-s"
	        }
		}).click(function(){
			$("#jm-options-menu").hide();
			var menu = $("#jm-actions-menu");
			if ( $(menu).is(':visible') ) {
				menu.hide();
			} else {
				if(JM.world.singlePlayer===true) {
					$("#autoMapButton").removeClass('ui-state-disabled');
				} else {
					$("#autoMapButton").addClass('ui-state-disabled');
				}
				menu.show();
				$( document ).one( "click", function() { menu.hide(); });
			}
	        return false;
		});
		
		// Save map button
		$("#saveButton").attr("title", getMessage('save_button_title')).click(function() {
			saveMapImage();
		});
		$("#saveButtonText").html(getMessage('save_button_text'));
		
		// Automap button
		$("#autoMapButton").attr("title", getMessage('automap_title')).click(function() {
			if(JM.world.singlePlayer===true) {
				autoMapDialog();
			} else {
				return false;
			}
		});
		$("#autoMapButtonText").html(getMessage('automap_text'));				
		
		// World info
		$("#worldInfo").hide();
		$("#worldNameLabel").html(getMessage('worldname_text'));
		$("#worldTimeLabel").html(getMessage('worldtime_text'));
		$("#playerBiomeLabel").html(getMessage('biome_text'));
		$("#playerLocationLabel").html(getMessage('location_text'));
		$("#playerElevationLabel").html(getMessage('elevation_text'));
		
		// Disable selection on nav elements
		$(".nav").disableSelection();

		// Set flag so this function doesn't get called twice
		uiInitialized = true;

		// Continue
		initWorld();

	}
	
	var enforceFeature = function(buttonId, feature) {
		if(feature!==true) {
			$(buttonId).attr('disabled', true).parent().find('span').addClass('ui-state-disabled');
			$(buttonId).parent().children().css('cursor','not-allowed');
		} else {
			$(buttonId).removeAttr('disabled').parent().find('span').removeClass('ui-state-disabled');
			$(buttonId).parent().children().css('cursor','pointer');
		}
	}
	
	/**
	 * Post preference to server
	 */
	var postPreference = function(prefName, prefValue) {

		$.ajax({
		  type: "POST",
		  url: "/properties",
		  data: prefName +"="+ prefValue
		}).fail(function(data, error, jqXHR){
			if (debug)
				console.log(">>> postPreference failed: " + data.status, error);
		}).done(function(){
			if (debug)
				console.log(">>> postPreference done: " + prefName);
		});
	}
	
	/**
	 * Set text and title on same object
	 */
	var setTextAndTitle = function(selector, text, title) {
		$(selector).html(getMessage(text)).prop('title', getMessage(title));
	}

	/**
	 * Initialize World.
	 * 
	 * @returns
	 */
	var initWorld = function() {

		if (debug)
			console.log(">>> " + "initWorld");

		// Reset state
		halted = false;
		updatingMap = false;
		playerUnderground = false;
		queryServerPending = 0;
		clearTimer();		
		
		JM.mobs = {};
		JM.animals = {};
		JM.players = {};
		JM.villagers = {};
		JM.waypoints = {};
		JM.images = {};
		
		markers = {
			mobs : {},
			animals : {},
			players : {},
			villagers : {},
			waypoints : {},
			player: {}
		}

		queryServer(setupMap);

	}

	var setupMap = function() {
		
		// Google Map
		map = initMap($('#map-canvas')[0]);
		
		map.controls[google.maps.ControlPosition.TOP_LEFT].push($('#jm-logo-span')[0]);
		map.controls[google.maps.ControlPosition.TOP_CENTER].push($('#jm-toolbar')[0]);
		map.controls[google.maps.ControlPosition.TOP_CENTER].push($('#jm-toggles')[0]);
		map.controls[google.maps.ControlPosition.TOP_CENTER].push($('#jm-rt-menus')[0]);
		map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push($('#worldInfo')[0]);
					
		google.maps.event.addListener(map, 'click', function(event) {
	    	var projection = map.getProjection(); 
	    	var coords = projection.fromPointToLatLng(event.pixel);
	        // console.log(coords); TODO show coords with mouseover
    	});
	    		
		// Close error
		if(errorDialog) {
			$('.ui-dialog').remove();
			errorDialog = null;
		}
		
		// Close splash
		if(splashDialog) {
			$(splashDialog).parent().delay(1000).fadeOut(1000, function(){
				$('.ui-dialog').remove();
				splashDialog = null;
			});
		}

		setCenterOnPlayer(true);
		
		// Show update button
		if (JM.world.latest_journeymap_version > JM.world.jm_version) {
			window.setTimeout(function(){
				var text = getMessage('update_button_title');
				text = text.replace("%1$s", JM.world.latest_journeymap_version);
				text = text.replace("%2$s", JM.world.mc_version);
				var onClickFn = function(e){
					var url = $('#webLink')[0].href;
					window.open(url, '_new', '');
				};
				$("#jm-update-button").button()
					.attr("title", text)
					.click(onClickFn);
				
				map.controls[google.maps.ControlPosition.TOP_CENTER].push($('#jm-alerts')[0]);
				
				showInfoMessage(text, 2500, onClickFn);
				
			}, 3000);
		}

	}

	/**
	 * Invoke saving map file
	 */
	var saveMapImage = function() {
		_gaq.push(['_trackEvent', 'saveMapImage', null, 0, true]);
		
		$.ajax({
			  type: 'POST',
			  url: '/action?type=savemap' + getMapStateUrl(),
			  dataType : 'jsonp'
		}).fail(function(data, error, jqXHR){
			//if (debug)
				console.log(">>> saveMapImage failed: " + JSON.stringify(data));
			var msg = 'Error: ' + JSON.stringify(data);
			showInfoMessage(msg, 2000);
		}).success(function(data){
			console.log('automap result: ', data);
			var msg = getMessage('save_filename', data.filename);
			showInfoMessage(msg, 2000);
		});	
	}
	
	/**
	 * Prompt automap
	 */
	var autoMapDialog = function() {
		
		var dialog = $(messageTemplate)
			.html(getMessage('automap_dialog_text'))
			.css('z-index', google.maps.Marker.MAX_ZINDEX + 1)
			.css('min-height', '20px')
			.dialog({ 
			modal: true,
			buttons: [ 
			   { width:'80px', text: getMessage('automap_dialog_all'),     click: function() { toggleAutoMap('all');  $( this ).dialog( "close" ); } },
			   { width:'80px', text: getMessage('automap_dialog_missing'), click: function() { toggleAutoMap('missing');  $( this ).dialog( "close" ); } },
	           { width:'80px', text: getMessage('automap_dialog_none'),  click: function() { toggleAutoMap('stop');  $( this ).dialog( "close" ); } },
	           { width:'80px', text: getMessage('automap_dialog_close'),  click: function() { $( this ).dialog( "close" ); } }
			]
		}).parent().find('.ui-dialog-titlebar').remove();
		$(dialog).find('.ui-dialog-buttonset').css('width','100%');
				
	}

	/**
	 * Invoke starting auto-map
	 */
	var toggleAutoMap = function(scope) {
		_gaq.push(['_trackEvent', 'toggleAutoMap', null, 0, true]);
		$.ajax({
			  type: "POST",
			  url: "/action?type=automap&scope=" + scope,
			  dataType : "jsonp"
		}).fail(function(data, error, jqXHR){
			//if (debug)
				console.log(">>> toggleAutoMap failed: " + JSON.stringify(data));
			var msg = 'Error: ' + JSON.stringify(data);
			showInfoMessage(msg, 2000);
		}).success(function(data){
			console.log('automap result: ', data);
			var msg = getMessage(data.message);
			showInfoMessage(msg, 2000);
		});	
	}
	
	var showInfoMessage = function(msg, duration, callback) {
		
		var message = $(messageTemplate).dialog({ modal: false });
		
		var closeFn = function(){
			$(message).remove();
			message = null;
		}
		
		$(message).css('z-index', google.maps.Marker.MAX_ZINDEX + 1)
			   .css('min-height', '20px')
			   .html(msg)
			   .click(function(){
				   if(callback) callback();
				   closeFn();
			   })
			   .parent().find('.ui-dialog-titlebar').remove();
		
		$(message).parent().delay(2000).fadeOut(1500, closeFn);
	}

	function setMapType(mapType) {

		var typeChanged = false;
		if (mapType === "day") {
			if (isNightMap === false) return;
			isNightMap = false;
			typeChanged = true;

			$("#dayNightText").html(getMessage('day_button_text'));
			$("#dayNightButton").attr("title", getMessage('day_button_title'));
			$("#dayNightButtonImg").attr('src', '/theme/icon/day.png')

		} else if (mapType === "night") {
			if (isNightMap === true) return;
			isNightMap = true;
			typeChanged = true;

			$("#dayNightText").html(getMessage('night_button_text'));
			$("#dayNightButton").attr("title", getMessage('night_button_title'));
			$("#dayNightButtonImg").attr('src', '/theme/icon/night.png');
	
		} else {
			if (debug)
				console.log(">>> " + "Error: Can't set mapType: " + mapType);
		}
		
		if(typeChanged && playerUnderground === false) {		
			if (debug) console.log("setMapType(" + mapType + ")");
			refreshMap();
		}

	}

	function setCenterOnPlayer(onPlayer) {	
		centerOnPlayer = onPlayer;
		
		if(onPlayer) {
			$("#followButtonImg").attr('src', '/theme/icon/follow.png');
			if(markers.playerMarker) {
				map.panTo(markers.playerMarker.getPosition());
				drawPlayer();
			} else {
				refreshMap();
			}	
		} else {
			$("#followButtonImg").attr('src', '/theme/icon/follow.png');
		}

	}

	function setShowCaves(show) {

		if (show === showCaves) {
			return;
		}
		showCaves = show;

		if (playerUnderground === true) {
			refreshMap();
		}		

	}

	// //////////// DATA ////////////////////

	var queryServer = function(callback) {

		if (halted === true)
			return;
		
		if(queryServerPending>2) {
			// time to reset
			if(debug) console.log("Too many queries pending, resetting.");
			window.location = window.location;
		}
		
		// Params for dirty image check
		var params = "";
		if(map) {
			if(!lastImageCheck) lastImageCheck = new Date().getTime();
			params = "?images.since=" + lastImageCheck;
		}

		// Get all the datas
		queryServerPending++;
		fetchData("/data/all" + params, function(data) {
			queryServerPending--;
			
			// Apply data
			JM.animals = data.animals;
			JM.images = data.images;
			JM.mobs = data.mobs;
			JM.player = data.player;
			JM.players = data.players;
			JM.villagers = data.villagers;
			JM.waypoints = data.waypoints;
			JM.world = data.world;
			
			// Update underground state
			var wasUnderground = playerUnderground;
			playerUnderground = JM.player.underground;

			// Update UI
			$("#playerBiome").html(JM.player.biome);
			$("#playerLocation").html(Math.round(JM.player.posX) + "," + Math.round(JM.player.posZ));
			$("#playerElevation").html(Math.round(JM.player.posY) + "&nbsp;(" + (JM.player.posY >> 4) + ")");

			// 0 is the start of daytime, 12000 is the start of sunset, 13800 is
			// the start of nighttime, 22200 is the start of sunrise, and 24000 is
			// daytime again.
			var allsecs = JM.world.time / 20;
			var mins = Math.floor(allsecs / 60);
			var secs = Math.ceil(allsecs % 60);
			if (mins < 10)
				mins = "0" + mins;
			if (secs < 10)
				secs = "0" + secs;
			var currentTime = mins + ":" + secs;

			// Update UI elements
			$("#worldName").html(unescape(JM.world.name).replace("\\+", " "));
			$("#worldTime").html(currentTime);
			$("#worldInfo").show();					

			// Set map type based on time
			if (playerOverrideMap != true) {
				if (JM.world.dimension === 0 && playerUnderground === false) {
					if (JM.world.time < 13800) {
						setMapType('day');
					} else {
						setMapType('night');
					}
				}
			}

			if (halted === true)
				return;

			// Draw the map
			if(map) {
				if(wasUnderground !== playerUnderground) {
					refreshMap();
				} else {
					mapOverlay.refreshTiles();
				}
				drawMap();
			}

			if (timerId === null) {
				var dur = (JM.world && JM.world.browser_poll) ? JM.world.browser_poll : 1000;
				timerId = setInterval(queryServer, Math.max(1000, dur));
			}
			
			if (callback) {
				callback();
			}

		});

	}

	/**
	 * Fetch JsonP data. Generic error handling, callback invoked on success
	 */
	var fetchData = function(dataUrl, callback) {

		if (debug)
			console.log(">>> " + "fetchData " + dataUrl);

		$.ajax({
			url : dataUrl,
			dataType : "jsonp"
		}).fail(handleError).done(callback);
	}

	
	/**
	 * Force immediate update
	 */
	var refreshMap = function() {
		if (debug) console.log(">>> " + "refreshMap");
			
		if(!map) return;
		
		delay(function(){			
			if (debug) console.log(">>> " + "delayed refreshMap");
			
			lastImageCheck = 1;
			var zoom = map.getZoom();		
			var delta = (zoom==MapConfig.maxZoom) ? -0.0000001 : 0.0000001 ;
			var center = map.getCenter();
			
			// This hack forces the tiles to be replaced but doesn't visibly change the map
			map.setZoom(zoom + delta);
			map.panTo(center);
			map.setZoom(zoom);
			map.panTo(center);
		}, 500 );

	}

	/**
	 * Clear the timer
	 */
	var clearTimer = function() {
		if (timerId !== null) {
			clearInterval(timerId);
			timerId = null;
		}
	}
	
	/**
	 * Get L10N message by key
	 */
	var getMessage = function(key, params) {
		if(!JM.messages || !JM.messages[key]) {
			console.log("Missing L10N message: " + key);
			return "!" + key + "!";
		} else {
			var msg = JM.messages[key];
			if(!params) {
				return msg;
			} else {
				if(!Array.isArray(params)){
					params = [params];
				}
				return msg.format.apply(msg, params);
			}
		}
	}

	// Ajax request got an error from the server
	var handleError = function(data, error, jqXHR) {

		if (debug)
			console.log(">>> " + "handleError");

		// Secondary errors will be ignored
		if (halted === true)
			return;
		
		_gaq.push(['_trackEvent', 'handleError', JSON.stringify(data), 0, true]);

		// Clear the timer
		clearTimer();
		queryServerPending = false;

		if(console && console.log) console.log("Server returned error: " + JSON.stringify(data));

		// Move nav components back to holder
		$(".nav").appendTo( $("#nav-holder") );
		
		// Destroy Google Map
		$("#map-canvas").empty();
		
		// Ensure splash destroyed
		if(splashDialog) {
			$(splashDialog).remove();
			splashDialog = null;
		}

		// Display error
		var displayError;
		if (data.status === 503 || data.status === 0) {
			displayError = getMessage('error_world_not_opened');
		} else {
			displayError = "";
		}
		
		if(!errorDialog) {			
			errorDialog = createDialog(displayError, true);
		}
		$(errorDialog).find('div').html(displayError);
			
		// Restart in 5 seconds
		if (!halted) {
			halted = true;
			if (console)
				console.log(">>> " + "Will re-check game state in 5 seconds.");
			
			setTimeout(function() {
				document.location = document.location;
			}, 5000);
		}
	}
	
	var createDialog = function(text, modal) {
		if(!modal) modal=false;
		var dialog = $(dialogTemplate).dialog({ modal: modal });
		dialog.parent().find('.ui-dialog-titlebar').remove();
		dialog.css('z-index', google.maps.Marker.MAX_ZINDEX + 1);
		if(text) {
			dialog.find('div').html(text);
		}
		return dialog;
	}

	// ////////////DRAW ////////////////////

	// Draw the map
	var drawMap = function() {		

		if (debug)
			console.log(">>> " + "drawMap");

		if (drawingMap === true) {
			if (debug)
				console.log(">>> " + "Avoided concurrent drawMap()");
			return false;
		}

		drawingMap = true;

		// mobs
		drawMobs();

		// other players
		drawMultiplayers();
		
		// waypoints
		drawWaypoints();

		// player
		drawPlayer();

		drawingMap = false;

	}

	// Draw the player icon
	var drawPlayer = function() {
		
		if (debug)
			console.log(">>> " + "drawPlayer");
		
		// Get current player position
		var pos = blockPosToLatLng(JM.player.posX, JM.player.posZ);
		var heading = JM.player.heading;
		var imgId = 'player'+JM.player.entityId;
		
		// Ensure marker
		if(!markers.playerMarker) {

			var img = new Image();
			$(img).attr('id', imgId)
			      .attr('src','/img/locator-player.png')
			      .css('width','64px')
			      .css('height','64px')
			      .rotate(heading);
			
			markers.playerMarker = new RichMarker({
				position: pos,
			    map: map,
			    draggable: false,
			    flat: true,
			    anchor: RichMarkerPosition.MIDDLE,
			    content: img,
			    zIndex: google.maps.Marker.MAX_ZINDEX + 1,
			    tooltip : new RichMarker({
					position: null,
				    map: null,
				    draggable: false,
				    flat: true,
				    anchor: RichMarkerPosition.BOTTOM,
				    content: '<div class="playerInfo">' + JM.player.username + '</div>',
		        })
	        });
			
			google.maps.event.addListener(markers.playerMarker, 'mouseover', function(args) {
				var tooltip = markers.playerMarker.tooltip;
				if(tooltip.timeout) {
					window.clearTimeout(tooltip.timeout);
				}
				if(!tooltip.getMap()) {
					tooltip.setPosition(markers.playerMarker.position);
					tooltip.setMap(markers.playerMarker.map);
					tooltip.setZIndex(google.maps.Marker.MAX_ZINDEX + 1);
				}
			});
			
			google.maps.event.addListener(markers.playerMarker, 'mouseout', function(args) {
				
				var tooltip = markers.playerMarker.tooltip;
				if(tooltip.getMap()) {
					if(tooltip.timeout) {
						window.clearTimeout(tooltip.timeout);
					}
					tooltip.timeout = window.setTimeout(function(){
						delete tooltip.timeout;
						tooltip.setMap(null);
					}, 500)					
				}
			});
			

			google.maps.event.addListener(map, 'dragstart', function() {
				setCenterOnPlayer(false);
			});
			
			google.maps.event.addListener(map, 'zoom_changed', function() {
				if(centerOnPlayer===true) {
					if(markers.playerMarker) {
						map.panTo(markers.playerMarker.getPosition());
					}
				}
				
				if(markers.waypoints) {
					drawWaypoints();
				}
				
				if(map.getZoom()==0) {
					$('img.entityLocator').css('visibility','hidden');
				} else {
					$('img.entityLocator').css('visibility','visible');
				}
				
				if(map.getZoom()==0) {
					$('img.entityMarker').css('visibility','hidden');
				} else {
					$('img.entityMarker').css('visibility','visible');
				}
				
			});

		} 
		
		// Update marker position and heading
		markers.playerMarker.setPosition(pos);
		$('#'+imgId).rotate(heading);
		
		// Keep on top
		if(markers.playerMarker.getZIndex()<google.maps.Marker.MAX_ZINDEX){
			markers.playerMarker.setZIndex(google.maps.Marker.MAX_ZINDEX + 1);
		}
		
		// Center if needed
		if(centerOnPlayer===true) {
			map.panTo(pos);
		}
	};
	
	// Remove markers not in current JM data
	var removeObsoleteMarkers = function(jmMap, markerMap) {
		$.each(markerMap, function(id, marker) {
			if(!jmMap || !(id in jmMap) ){
				marker.setMap(null);
				if(debug) console.log("Marker removed for " + id);
				delete markerMap[id];
			}
		});
	};

	// Draw the location of mobs
	var drawMobs = function() {

		if (debug)
			console.log(">>> " + "drawMobs");
		
		if(map.getZoom()===0) return;
		
		// Mobs
		removeObsoleteMarkers(JM.mobs, markers.mobs);		
		if (JM.world.features.RadarMobs && showMobs === true && JM.mobs) {
			$.each(JM.mobs, function(index, mob) {
				updateEntityMarker(mob,markers.mobs);
			});
		}

		// Animals
		removeObsoleteMarkers(JM.animals, markers.animals);	
		if (JM.world.features.RadarAnimals && (showAnimals === true || showPets === true) && JM.animals) {
			$.each(JM.animals, function(index, mob) {
				updateEntityMarker(mob,markers.animals);
			});
		}

		// Villagers
		removeObsoleteMarkers(JM.villagers, markers.villagers);	
		if (JM.world.features.RadarVillagers && showVillagers === true && JM.villagers) {
			$.each(JM.villagers, function(index, mob) {
				updateEntityMarker(mob,markers.villagers);
			});
		}
		
	};

	// Create or update marker
	var updateEntityMarker = function(entity, markerMap) {

		// Check current entity position		
		var pos = blockPosToLatLng(entity.posX, entity.posZ);
		if(!map.getBounds().contains(pos)) {
			//return; // Don't bother with marker
		}
		
		var id = 'id' + entity.entityId;
		var heading = entity.heading;

		var locatorUrl = null;
		var iconSize = 32;
		var iconColor = "#cccccc";
		var iconLabel = null;
		var marker = markerMap[id];

		var isPet = (entity.owner!=null && entity.owner!="");

        if(entity.owner === JM.player.username) {
            locatorUrl = "/img/locator-pet.png";
            iconColor = "#0000ff";
        } else if(entity.hostile==true) {
            locatorUrl = "/img/locator-hostile.png";
            iconColor = "#ff0000";
        } else {
            locatorUrl = "/img/locator-neutral.png";
        }

		if(!showPets && isPet==true) {
            locatorUrl = null;
        }

        if(!showAnimals && entity.passiveAnimal==true) {
            if(!(isPet==true && showPets)) {
                locatorUrl = null;
            }
        }
		
		// No locator means no marker
		if(!locatorUrl) {
			if(marker) {
				marker.setMap(null);
				delete markerMap[id];
				if(debug) console.log("Marker invalid for " + id + " - " + entity.filename);
				return;
			} else {
				//if(debug) console.log("Pending marker invalid for " + id + " - " + entity.filename);
				return;
			}		
		}

		// Create marker if needed
		if(!marker) {	
			
			var contentDiv = $(entityTemplate).attr('id',id);
					
			marker = new RichMarker({
				position: pos,
			    map: map,
			    draggable: false,
			    flat: true,
			    anchor: RichMarkerPosition.MIDDLE,
			    content: contentDiv[0]
	        });
			markerMap[id] = marker;
			
			if(debug) console.log("Marker added for " + id);
		}
	
		// Label if customName exists
		var contentDiv = $('#'+id);
		
		if(entity.customName) {
			$(contentDiv).find('.entityName').css('visibility','visible').html(entity.customName);
		} else {
			$(contentDiv).find('.entityName').css('visibility','hidden').html();
		}
		
		// Entity icon
		$(contentDiv).find('.entityIcon').addClass('entity_' + JM.world.iconSetName).attr('src','/icon/entity/' + JM.world.iconSetName + '/' + entity.filename);

		// Entity locator		
		$(contentDiv).find('.entityLocator').attr('src', locatorUrl).rotate(heading);		
	
		// Update marker position
		marker.setPosition(pos);
	};

	// Draw the location of other players
	var drawMultiplayers = function() {

		if (debug)
			console.log(">>> " + "drawMultiplayers");

		// Villagers
		removeObsoleteMarkers(JM.players, markers.players);	
		if (showPlayers === true && JM.players) {
			$.each(JM.players, function(index, multiplayer) {
				updateMultiplayerMarker(multiplayer,markers.players);
			});
		}
		
	};
	
	// Create or update marker
	var updateMultiplayerMarker = function(multiplayer, markerMap) {

		// Check current multiplayer position		
		var pos = blockPosToLatLng(multiplayer.posX, multiplayer.posZ);

		var id = multiplayer.username;
		var heading = multiplayer.heading;
		var marker = markerMap[id];

		// Create marker if needed
		if(!marker) {	
			var contentDiv = $(mpTemplate).attr('id',id);
			$(contentDiv).find('.mpName').html(id);
			$(contentDiv).find('.mpIcon')
				.attr('src', multiplayer.filename)
				.on('error', function(event){
					if(debug) console.log("Skin not found for " + id);
					$(event.target).attr('src', '/img/entity/unknown.png');					
				});
			$(contentDiv).find('.mpLocator').rotate(heading);
					
			marker = new RichMarker({
				position: pos,
			    map: map,
			    draggable: false,
			    flat: true,
			    anchor: RichMarkerPosition.MIDDLE,
			    content: contentDiv[0]
	        });
			markerMap[id] = marker;
			
			if(debug) console.log("Marker added for " + id);
		}
	
		// Label if customName exists
		var contentDiv = $('#'+id);

		// multiplayer locator		
		$(contentDiv).find('.mpLocator').rotate(heading);		
	
		// Update marker position
		marker.setPosition(pos);
	};
	
	// Draw the location of waypoints
	var drawWaypoints = function() {
		
		if (debug)
			console.log(">>> " + "drawWaypoints");


		removeObsoleteMarkers(JM.waypoints, markers.waypoints);
		
		if(!showWaypoints==true || !JM.waypoints) {
			return;
		}	

		$.each(JM.waypoints, function(index, waypoint) {
			updateWaypointMarker(waypoint,markers.waypoints);
		});
			
	};
	
	// Create or update marker
	var updateWaypointMarker = function(waypoint, markerMap) {

		// Get current waypoint position
        var x = dimensionalValue(waypoint.x, waypoint.primaryDimension, JM.player.dimension);
        var z = dimensionalValue(waypoint.z, waypoint.primaryDimension, JM.player.dimension);
        var pos = blockPosToLatLng(x, z);

		if(!map.getBounds().contains(pos)) {
			//return; // Don't bother with marker
			// TODO: Put on edge of map as an arrow?
		}

        var id = waypoint.id;
        waypoint.color = rgbToHex(waypoint.r, waypoint.g, waypoint.b);

        var marker = markerMap[id];
		
		if(!marker) {
			var icon = {
				fillOpacity: 0.85,
				scale: 1,
				fillColor: waypoint.color,
				strokeColor: 'white'
			};
			var labelClass = "waypoint";

			if(waypoint.type=="Death") {
				// death point: X marks the spot
				icon.path = 'M -10,-10 0,-4 10,-10 14,-8 4,0 14,8 10,10 0,4 -10,10 -14,8 -4,0 -14,-8 z';
				icon.strokeWeight = 1.5;
				labelClass = labelClass + " death";
			} else {
				// diamond
				icon.path = 'M 0,-8 8,0 0,8 -8,0 0,-8 z';
				icon.strokeWeight = 1.5;
				labelClass = labelClass;
			}
			
			var title = [
			    getMessage('location_text'),
			    waypoint.x + "," + waypoint.z,
			    getMessage('elevation_text'),
			    waypoint.y,
			    "(" + (waypoint.y >> 4) + ")"
			].join(' ');
			
			var label = waypoint.name;
			var titleSpan = $('<span/>').addClass(labelClass).html(label).hide().appendTo(document.body);
			var titleWidth = 4 + $(titleSpan).innerWidth()/2;
			$(titleSpan).remove();
			//console.log("titleWidth " + titleWidth);

			marker = new MarkerWithLabel({
		       position: pos,
		       map: map,
		       draggable: false,
		       clickable: false,
		       icon: icon,
		       title: title,
		       labelContent: label,
		       labelClass: labelClass,
		       labelAnchor: new google.maps.Point(titleWidth,40)
		     });
			markerMap[id] = marker;	
			
			if(debug) console.log("Marker added for " + id);
			
		} else {
			
			// Update marker color		
			if(marker.icon.fillColor!==waypoint.color) {
				marker.icon.fillColor = waypoint.color;
				marker.setIcon(marker.icon);
			}
			
			//console.log("Setting position of waypoint: ", pos);
			marker.setPosition(pos);
		}
	};

    var dimensionalValue = function(original, primaryDimension, dimension) {
        if(primaryDimension==dimension)
        {
            return original;
        }
        else if(primaryDimension==-1)
        {
            return original*8;
        }
        else if(dimension==-1)
        {
            return original/8;
        }
        else
        {
            return original;
        }
    };
	
	var getMapStateUrl = function() {
		var mapType = (JM.world.features.MapCaves === true && playerUnderground === true && showCaves === true) ? "underground" : (isNightMap === true ? "night" : "day");
		var dimension = (JM.player.dimension);
		var depth = (JM.player && JM.player.chunkCoordY != undefined) ? JM.player.chunkCoordY : 4;
		return "&mapType=" + mapType + "&dim=" + dimension + "&depth=" + depth + "&ts=" + lastImageCheck;
	};
	
	var rgbToHex = function(r, g, b) {
	    return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
	}
	
	var getURLParameter = function(name) {
		return decodeURI((RegExp(name + '=' + '(.+?)(&|$)').exec(location.search) || [, null])[1]);
	}
	
	debug = ('true' === getURLParameter('debug'));
	
	/** Google Maps Code **/
	
	google.maps.visualRefresh = true;
	
	var MapConfig = {
		tileSize : 512,
		defaultZoom : 0,
		minZoom : 0,
		maxZoom : 5
	}
	MapConfig.perPixel = 1.0 / MapConfig.tileSize;
	
	var blockPosToLatLng = function (x, y) {
		var me = this;		
		var center = .5 * MapConfig.perPixel;		
		var lat = (y * MapConfig.perPixel) + center;
		var lng = (x * MapConfig.perPixel) + center;		
		return new google.maps.LatLng(lat, lng);
	}
	
	var initMap = function (container) {
		
		var pos;
		if(JM.player && JM.player.posX) {
			pos = blockPosToLatLng(JM.player.posX, JM.player.posZ);
		} else {
			pos = new google.maps.LatLng(0,0);
		}
		
		map = new google.maps.Map(container, {
	        zoom: MapConfig.defaultZoom,
	        center: pos,
	        mapTypeControl: false,
	        streetViewControl: false,
	        panControl: true,
		    panControlOptions: {
		        position: google.maps.ControlPosition.LEFT_TOP
		    },
		    zoomControl: true,
		    zoomControlOptions: {
		        style: google.maps.ZoomControlStyle.AUTO,
		        position: google.maps.ControlPosition.LEFT_TOP
		    }
	    });
		
	    mapOverlay = new MCMapType();
	    map.mapTypes.set('jm', mapOverlay);
	    map.setMapTypeId('jm');	  

	    return map;
	}

	var MCMapType = function () {
		this.loadedTiles = {};
		this.projection = new MCMapProjection(MapConfig.tileSize);
		this.tileSize = new google.maps.Size(MapConfig.tileSize,MapConfig.tileSize);
		this.minZoom = MapConfig.minZoom;
		this.maxZoom = MapConfig.maxZoom;
		//this.isPng = true;
	};	
	
	// Adapted from http://code.martinpearman.co.uk/deleteme/MyOverlayMap.js
	MCMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
		var me = this;

		zoom = Math.floor(zoom);

		while (coord.x > 200)
		{
			coord.x -= 360;
		}
		var tileUrl = "/tile?zoom=" + zoom + "&x=" + coord.x + "&z=" + coord.y;
		var tileId = 'x_' + coord.x + '_y_' + coord.y + '_zoom_' + zoom;
		
		var tile = ownerDocument.createElement('div');
		$(tile).css('width', this.tileSize.height + 'px')
			   .css('width', this.tileSize.height + 'px')
			   .css('height', this.tileSize.height + 'px')
			   .attr('data-tileid', tileId);
		
		if (debug) {
			var label = ownerDocument.createElement('span');
			$(label).css('color','white')
			        .css('float','left')
			        .html(coord.toString() + " zoom " + zoom);
			$(tile).append(label);
		}
		
		var img = $('<img>')
			.attr('src', tileUrl += getMapStateUrl());
		
		if(img.width()>0) {
			$(tile).prepend(img);
		} else {
			$(img).on('load', function() {
				$(tile).prepend(img);
			});
		}
			
		me.loadedTiles[tileId] = {
			tile: tile,
			tileUrl: tileUrl,
			coord: coord,
			zoom: zoom
		}
			
		return tile;
	};
	
	MCMapType.prototype.refreshTile = function(tile) {
		var me = this;
		
		if (debug) console.log(">>> " + "refreshTile " + $(tile).data('tileid'));
		
		var tileData = me.loadedTiles[$(tile).data('tileid')];
		if(tileData) {
			var url = tileData.tileUrl + getMapStateUrl();			
			$(tile).find('img').attr('src', url);
		}
	}
	
	MCMapType.prototype.refreshTiles = function (force) {
		var me = this;
		
		if(!force) force==false;
		if (debug) console.log(">>> " + "refreshTiles: " + force);
		
		if(force) {			
			for (var tileId in me.loadedTiles) {
				var tileData = me.loadedTiles[tileId];
				var tile = tileData.tile;
				me.refreshTile(tile);
			}
			lastImageCheck = JM.images.queryTime;
			return;
		}
			
		if(JM.images.regions.length===0) {
			if (debug) console.log("No regions have changed: ", JM.images);
			lastImageCheck = JM.images.queryTime || new Date().getTime();
			return;
		}
		
		lastImageCheck = JM.images.queryTime || new Date().getTime();
		
		if (debug) {
			console.log("Regions changed since ", JM.images.since);
			JM.images.regions.forEach(function(region) {
				console.log("\t", region);
			});
		}

		for (var tileId in me.loadedTiles) {
			
			var tileData = me.loadedTiles[tileId];
			if(!tileData) continue;
			
			var tile = tileData.tile;
			var zoom = tileData.zoom;
			var scale = Math.pow(2,zoom);
			var coord = tileData.coord;

			while (coord.x > 200)
			{
				coord.x -= 360;
			}
			var tileRegion = [parseInt(coord.x / scale), parseInt(coord.y / scale)];
						
			JM.images.regions.forEach(function(region) {
				if(tileRegion[0]==region[0] && tileRegion[1]==region[1]) {
					if (debug) console.log("    tile " + coord + " zoom " + zoom + " in region: ", tileRegion);
					me.refreshTile(tile);
					return false;
				} else {
					if (debug) console.log("    tile " + coord + " zoom " + zoom + " not in region: ", tileRegion);
					return true;
				}
			});				
		}
		
	};
	
	MCMapType.prototype.releaseTile = function (tile) {
		delete this.loadedTiles[tile.tileId];
		tile = null;
	};
	
	// Public API for JourneyMap
	return {
		start : initMessages
	};
			
})();

/** Custom Map Projection **/
function MCMapProjection(tileSize) {
	this.tileSize = tileSize;
    this.inverseTileSize = 1.0 / tileSize;
}
  
MCMapProjection.prototype.fromLatLngToPoint = function(latLng) {
	var me = this;
	var x = latLng.lng() * me.tileSize;
	var y = latLng.lat() * me.tileSize;
	return new google.maps.Point(x, y);
};

MCMapProjection.prototype.fromPointToLatLng = function(point) {
	var me = this;
    var lng = point.x * me.inverseTileSize;
    var lat = point.y * me.inverseTileSize;
    return new google.maps.LatLng(lat, lng);
};

/** Google Analytics **/
var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-28839029-1']);
_gaq.push(['_setDomainName', 'none']);
_gaq.push(['_setAllowLinker', true]);
_gaq.push(['_trackPageview']);

(function() {
	var ga = document.createElement('script');
	ga.type = 'text/javascript';
	ga.async = true;
	ga.src = ('https:' === document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0];
	s.parentNode.insertBefore(ga, s);
})();

function ga_heartbeat(){
  _gaq.push(['_trackEvent', 'heartbeat', 'heartbeat', null, 0, true]);
  setTimeout(ga_heartbeat, 5*60*1000);
}
ga_heartbeat();

/** String format **/
if (!String.prototype.format) {
  String.prototype.format = function() {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined'
        ? args[number]
        : match
      ;
    });
  };
} 

/** OnLoad **/
$( document ).ready(function() {
	_gaq.push(['_trackEvent', 'document', 'ready', null, 0, true]);
	JourneyMap.start();	
});

$(window).unload(function(){
	_gaq.push(['_trackEvent', 'document', 'unload', null, 0, true]);
});
