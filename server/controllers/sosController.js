const Sos = require('../models/Sos');

const triggerSos = async (req, res) => {
    try {
        const { latitude, longitude } = req.body;
        console.log(`[Backend API] /sos triggered with coordinates: Lat: ${latitude}, Lng: ${longitude}`);

        // In a real app, you would find the nearest ambulance and hospital here.
        // For now, we mock the assignment logic to match Android app's expectations.
        const mockSos = {
            sosId: `SOS-${Date.now()}`,
            active: true,
            stage: 'to_user',
            etaMin: 6,
            totalKm: 3.5,
            user: { latitude, longitude },
            ambulance: {
                id: 'AMB-2049', number: 'MH 12 AB 2049', driver: 'Rajesh Kumar', rating: 4.9, type: 'ALS', phone: '+919812345678',
                latitude: latitude + 0.018, longitude: longitude + 0.018
            },
            hospital: {
                id: 'H-mock', name: 'City General Hospital', speciality: 'Multi-Specialty', distanceKm: 3.2, beds: 12, phone: '108',
                latitude: latitude + 0.03, longitude: longitude + 0.01
            }
        };

        console.log(`[MongoDB Save] Saving SOS request to database...`);
        const state = await Sos.create(mockSos);
        console.log(`[API Response] Returning SOS state to frontend.`);
        res.status(200).json({ message: 'SOS Triggered', state });
    } catch (error) {
        res.status(500).json({ message: error.message, state: null });
    }
};

const getCurrentSos = async (req, res) => {
    try {
        const sos = await Sos.findOne({ active: true }).sort({ createdAt: -1 });
        res.status(200).json(sos); // If null, Android handles it
    } catch (error) {
        res.status(500).json(null);
    }
};

const updateAmbulancePosition = async (req, res) => {
    try {
        const { latitude, longitude, etaMin } = req.body;
        const sos = await Sos.findOne({ active: true }).sort({ createdAt: -1 });
        if (sos) {
            sos.ambulance.latitude = latitude;
            sos.ambulance.longitude = longitude;
            if (etaMin !== undefined) sos.etaMin = etaMin;
            await sos.save();
        }
        res.status(200).json({ message: 'Position updated' });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const updateStage = async (req, res) => {
    try {
        const { stage } = req.body;
        const sos = await Sos.findOne({ active: true }).sort({ createdAt: -1 });
        if (sos) {
            sos.stage = stage;
            await sos.save();
        }
        res.status(200).json({ message: 'Stage updated' });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const clearSos = async (req, res) => {
    try {
        await Sos.updateMany({ active: true }, { active: false });
        res.status(200).json({ message: 'SOS Cleared' });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

module.exports = { triggerSos, getCurrentSos, updateAmbulancePosition, updateStage, clearSos };
