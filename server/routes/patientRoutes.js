const express = require('express');
const router = express.Router();
const { addPatient, getAllPatients } = require('../controllers/patientController');

router.post('/add', addPatient);
router.get('/all', getAllPatients);

module.exports = router;
