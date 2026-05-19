const mongoose = require('mongoose');

const driverLocationSchema = mongoose.Schema(
    {
        ambulanceId: { type: String, required: true },
        driverUserId: { type: String, required: true },
        sosId: { type: String },
        latitude: { type: Number, required: true },
        longitude: { type: Number, required: true },
        speed: { type: Number, default: 0 },
        heading: { type: Number, default: 0 },
        stage: { type: String, default: 'dispatched' },
        timestampStr: { type: String }
    },
    {
        timestamps: true,
    }
);

module.exports = mongoose.model('DriverLocation', driverLocationSchema);
