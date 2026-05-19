const express = require('express');
const router = express.Router();
const { triggerSos, getCurrentSos, updateAmbulancePosition, updateStage, clearSos } = require('../controllers/sosController');

router.post('/', triggerSos);
router.get('/current', getCurrentSos);
router.post('/ambulance-position', updateAmbulancePosition);
router.post('/stage', updateStage);
router.post('/clear', clearSos);

module.exports = router;
