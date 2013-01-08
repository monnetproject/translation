-- MySQL dump 10.13  Distrib 5.5.24, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: test
-- ------------------------------------------------------
-- Server version	5.5.24-0ubuntu0.12.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `test`
--

DROP TABLE IF EXISTS `test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test` (
  `forin` varchar(255) DEFAULT NULL,
  `translation` varchar(255) DEFAULT NULL,
  `scores` varchar(255) DEFAULT NULL,
  `alignment` varchar(255) DEFAULT NULL,
  `extra` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test`
--

LOCK TABLES `test` WRITE;
/*!40000 ALTER TABLE `test` DISABLE KEYS */;
INSERT INTO `test` VALUES ('Digital ','Digital ','0.225352 0.191489 0.533333 0.3 2.718 ','','71 30'),('Digital ','digital fixed radio ','0.169637 0.037037 0.0791639 1.44835e-08 2.718 ','','14 30'),('Digital ','digital fixed ','0.169637 0.037037 0.0791639 0.00034321 2.718 ','','14 30'),('Digital ','digital ','0.0134228 0.037037 0.266667 0.35 2.718 ','','596 30'),('digital ','computerised form ','0.0150575 0.0015361 0.0557129 3.9125e-05 2.718 ','','37 10'),('digital ','computerised ','0.00157827 0.0015361 0.0557129 0.0625 2.718 ','','353 10'),('digital ','digital ','0.0134228 0.015873 0.8 0.5625 2.718 ','','596 10');
/*!40000 ALTER TABLE `test` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_de`
--

DROP TABLE IF EXISTS `test_de`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test_de` (
  `forin` varchar(255) DEFAULT NULL,
  `translation` varchar(255) DEFAULT NULL,
  `scores` varchar(255) DEFAULT NULL,
  `alignment` varchar(255) DEFAULT NULL,
  `extra` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_de`
--

LOCK TABLES `test_de` WRITE;
/*!40000 ALTER TABLE `test_de` DISABLE KEYS */;
INSERT INTO `test_de` VALUES ('FAMILIEN ','be ','4.62843e-06 1.7e-05 0.0348206 0.25 2.718 ','','120371 16'),('FAMILIEN ','families to be ','0.557129 0.00364623 0.0348206 0.0146484 2.718 ','','1 16'),('Familien ','/ her family ','0.0795898 0.004222 0.00389601 3.38112e-08 2.718 ','','7 143'),('Familien ','and families ','0.111426 0.264192 0.00389601 0.00594084 2.718 ','','5 143'),('Familien ','families , ','0.025324 0.264192 0.00389601 0.0601143 2.718 ','','22 143'),('Familien ','families experiencing ','0.557129 0.264192 0.00389601 3.51844e-06 2.718 ','','1 143'),('Familien ','families moving within ','0.557129 0.0885616 0.00389601 4.31646e-05 2.718 ','','1 143'),('Familien ','families of ','0.0174103 0.264192 0.00389601 0.105989 2.718 ','','32 143'),('Familien ','families ','0.313187 0.264192 0.797203 0.858156 2.718 ','','364 143'),('Familien ','families — the ','0.278564 0.132118 0.00389601 0.00101623 2.718 ','','2 143'),('Familien ','families — ','0.278564 0.132118 0.00389601 0.0121724 2.718 ','','2 143'),('Familien ','families … ','0.557129 0.264192 0.00389601 0.000133272 2.718 ','','1 143'),('Familien ','family breakdown ','0.139282 0.004222 0.00389601 1.12695e-06 2.718 ','','4 143'),('Familien ','family referred ','0.557129 0.0021227 0.00389601 0.000352095 2.718 ','','1 143'),('Familien ','family ','0.00734245 0.004222 0.0322452 0.0496454 2.718 ','','628 143'),('Familien ','following families ','0.278564 0.264192 0.00389601 0.00094749 2.718 ','','2 143'),('Familien ','her family ','0.0506481 0.004222 0.00389601 9.0156e-06 2.718 ','','11 143'),('Familien ','in families ','0.18571 0.264192 0.00389601 0.0248552 2.718 ','','3 143'),('Familien ','members ','9.24849e-05 0.0001446 0.00389601 0.0070922 2.718 ','','6024 143'),('Familien ','of families ','0.0557129 0.264192 0.00389601 0.105989 2.718 ','','10 143'),('Familien ','relatives ','0.0123806 0.0136986 0.00389601 0.0070922 2.718 ','','45 143'),('Familien ','the following families ','0.557129 0.264192 0.00389601 7.91022e-05 2.718 ','','1 143'),('Familien ','vegetables sector ','0.00640378 0.0003171 0.00389601 2.8078e-06 2.718 ','','87 143'),('Familien ','vegetables ','0.000260828 0.0003171 0.00389601 0.0070922 2.718 ','','2136 143'),('Familien ','— K.D. Chuck ','0.139282 0.500015 0.00389601 7.13465e-07 2.718 ','','4 143'),('Einkommensbesteuerung ','income tax ','0.0243581 0.00084345 0.791639 0.25 2.718 ','','65 2');
/*!40000 ALTER TABLE `test_de` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-10-10 22:56:03
