-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               10.4.25-MariaDB - mariadb.org binary distribution
-- Server OS:                    Win64
-- HeidiSQL Version:             12.1.0.6537
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Dumping structure for table adta.statistics_oee
DROP TABLE IF EXISTS `statistics_oee`;
CREATE TABLE IF NOT EXISTS `statistics_oee` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `machine_id` int(11) NOT NULL DEFAULT 1,
  `current_state` smallint(6) NOT NULL DEFAULT 0,
  `average_tput` int(11) NOT NULL DEFAULT 0,
  `max_3min_tput` int(11) NOT NULL DEFAULT 0,
  `successful_divert_packages` int(11) NOT NULL DEFAULT 0,
  `packages_inducted` int(11) NOT NULL DEFAULT 0,
  `tot_sec_since_reset` int(11) NOT NULL DEFAULT 0,
  `tot_sec_estop` int(11) NOT NULL DEFAULT 0,
  `tot_sec_fault` int(11) NOT NULL DEFAULT 0,
  `tot_sec_blocked` int(11) NOT NULL DEFAULT 0,
  `tot_sec_idle` int(11) NOT NULL DEFAULT 0,
  `tot_sec_init` int(11) NOT NULL DEFAULT 0,
  `tot_sec_run` int(11) NOT NULL DEFAULT 0,
  `tot_sec_starved` int(11) NOT NULL DEFAULT 0,
  `tot_sec_held` int(11) NOT NULL DEFAULT 0,
  `tot_sec_unconstrained` int(11) NOT NULL DEFAULT 0,
  `last_record` int(11) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- Dumping data for table adta.statistics_oee: ~0 rows (approximately)

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
