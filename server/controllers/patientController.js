const Patient = require('../models/Patient');

const addPatient = async (req, res) => {
    try {
        const patient = await Patient.create(req.body);
        res.status(201).json({ message: 'Patient added successfully', patient });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const getAllPatients = async (req, res) => {
    try {
        const patients = await Patient.find({});
        res.status(200).json(patients);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

module.exports = { addPatient, getAllPatients };
