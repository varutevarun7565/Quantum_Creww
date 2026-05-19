const express = require('express');
const router = express.Router();
const { registerDriver, pushDriverLocation, updateDriverStatus, getLatestLocation } = require('../controllers/driverController');

router.post('/register', registerDriver);
router.post('/location', pushDriverLocation);
router.put('/status', updateDriverStatus);
router.get('/:ambulanceId/location/latest', getLatestLocation);

module.exports = router;
