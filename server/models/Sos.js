const mongoose = require('mongoose');

const sosSchema = mongoose.Schema(
    {
        sosId: { type: String, required: true, unique: true },
        active: { type: Boolean, default: true },
        stage: { type: String, default: 'dispatched' }, // dispatched, to_user, at_user, to_hospital, at_hospital, completed, cancelled
        etaMin: { type: Number, default: null },
        totalKm: { type: Number, default: null },
        user: {
            latitude: { type: Number, required: true },
            longitude: { type: Number, required: true }
        },
        ambulance: {
            id: { type: String },
            number: { type: String },
            driver: { type: String },
            rating: { type: Number },
            type: { type: String },
            phone: { type: String },
            latitude: { type: Number },
            longitude: { type: Number }
        },
        hospital: {
            id: { type: String },
            name: { type: String },
            speciality: { type: String },
            distanceKm: { type: Number },
            phone: { type: String },
            beds: { type: Number },
            latitude: { type: Number },
            longitude: { type: Number }
        }
    },
    {
        timestamps: true,
    }
);

module.exports = mongoose.model('Sos', sosSchema);
