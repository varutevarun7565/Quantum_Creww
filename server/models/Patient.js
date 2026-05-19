const mongoose = require('mongoose');

const patientSchema = mongoose.Schema(
    {
        name: { type: String, required: true },
        condition: { type: String, required: true },
        time: { type: String, required: true },
        ambulanceId: { type: String, required: true },
        sosId: { type: String }
    },
    {
        timestamps: true,
    }
);

// Map _id to id for frontend
patientSchema.set('toJSON', {
    virtuals: true,
    versionKey: false,
    transform: function (doc, ret) {
        delete ret._id;
    }
});

module.exports = mongoose.model('Patient', patientSchema);
