<?php
//Sample Database Connection Syntax for PHP and MySQL.

//Connect To Database

//$arr = array('a' => 1, 'b' => 2, 'c' => 3, 'd' => 4, 'e' => 5);

//echo json_encode($arr);
/*
$arr = array( 
    array(
        "region" => "valore",
        "price" => "valore2"
    ),
    array(
        "region" => "valore",
        "price" => "valore2"
    ),
    array(
        "region" => "valore",
        "price" => "valore2"
    )
);

echo json_encode($arr);*/

//echo 'Hello ' . htmlspecialchars($_GET["name"]) . '!';

$lat = $_GET["lat"];
$long = $_GET["long"];
//echo 'lat'.($lat+5);

$hostname="10.6.175.43";
$username="SMARTMENU";
$password="ARdroid13!";
$dbname="SMARTMENU";
$usertable="RESTAURANT";
$yourfield = "name";

mysql_connect($hostname,$username, $password) or die ("<html><script language='JavaScript'>alert('Unable to connect to database! Please try again later.'),history.go(-1)</script></html>");
mysql_select_db($dbname);

# Check If Record Exists
/*SELECT * 
FROM  `RESTAURANT` 
WHERE (
latitude
BETWEEN 20 
AND 40
)
AND (
longitude
BETWEEN -140
AND -110
)
LIMIT 0 , 30*/
$query = "SELECT * FROM $usertable WHERE (latitude BETWEEN ($lat - 1) AND ($lat + 1)) AND (longitude BETWEEN ($long - 1) AND ($long + 1)) order by
ABS(ABS(ABS(latitude) - ABS($lat)) +
ABS(ABS(longitude)- ABS($long))) ASC LIMIT 0, 50";

$result = mysql_query($query);

$results = array();
if($result)
{
while($row = mysql_fetch_array($result))
{
	$name = $row["name"];
	$location_id = $row["location_id"];
	$latitude = $row["latitude"];
	$longitude = $row["longitude"];
	//echo "Name: ".$name."<br>";
	$results[] = array(
						"name"=>$name,
						"location_id"=>$location_id,
						"latitude"=>$latitude,
						"longitude"=>$longitude
						);
}
}

echo json_encode(array('results'=>$results));
?> 