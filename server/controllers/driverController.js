const DriverLocation = require('../models/DriverLocation');

const registerDriver = async (req, res) => {
    // In a real app, you would create a driver user here.
    res.status(200).json({ message: 'Driver registered' });
};

const pushDriverLocation = async (req, res) => {
    try {
        const { ambulanceId, driverUserId, sosId, latitude, longitude, speed, heading, stage } = req.body;
        const driverLoc = await DriverLocation.findOneAndUpdate(
            { ambulanceId },
            { driverUserId, sosId, latitude, longitude, speed, heading, stage, timestampStr: new Date().toISOString() },
            { new: true, upsert: true }
        );
        res.status(200).json({ message: 'Location updated', data: driverLoc });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const updateDriverStatus = async (req, res) => {
    res.status(200).json({ message: 'Status updated' });
};

const getLatestLocation = async (req, res) => {
    try {
        const { ambulanceId } = req.params;
        const loc = await DriverLocation.findOne({ ambulanceId });
        if (loc) {
            res.status(200).json(loc);
        } else {
            res.status(404).json({ message: 'Location not found' });
        }
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

module.exports = { registerDriver, pushDriverLocation, updateDriverStatus, getLatestLocation };
