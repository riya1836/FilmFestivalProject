import './Input.css';

export const Input = ({ 
  label, 
  error, 
  required,
  icon,
  className = '',
  ...props 
}) => {
  return (
    <div className={`form-group ${error ? 'has-error' : ''} ${className}`}>
      {label && (
        <label>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <div className="input-wrapper">
        {icon && <span className="input-icon">{icon}</span>}
        <input {...props} />
      </div>
      {error && <span className="error-message">{error}</span>}
    </div>
  );
};

export const Select = ({ 
  label, 
  error, 
  required,
  options = [],
  className = '',
  ...props 
}) => {
  const hasEmptyOption = options.some(opt => opt.value === '');
  
  return (
    <div className={`form-group ${error ? 'has-error' : ''} ${className}`}>
      {label && (
        <label>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <div className="select-wrapper">
        <select {...props}>
          {!hasEmptyOption && <option value="">Select {label?.toLowerCase()}</option>}
          {options.map(opt => (
            <option key={opt.value || 'empty'} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <span className="select-arrow">▼</span>
      </div>
      {error && <span className="error-message">{error}</span>}
    </div>
  );
};

export const Textarea = ({ 
  label, 
  error, 
  required,
  className = '',
  ...props 
}) => {
  return (
    <div className={`form-group ${error ? 'has-error' : ''} ${className}`}>
      {label && (
        <label>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <textarea {...props} />
      {error && <span className="error-message">{error}</span>}
    </div>
  );
};

export const Checkbox = ({ label, className = '', ...props }) => {
  return (
    <div className={`checkbox-group ${className}`}>
      <input type="checkbox" id={props.id} {...props} />
      {label && <label htmlFor={props.id}>{label}</label>}
    </div>
  );
};

export const Radio = ({ label, className = '', ...props }) => {
  return (
    <div className={`radio-group ${className}`}>
      <input type="radio" id={props.id} {...props} />
      {label && <label htmlFor={props.id}>{label}</label>}
    </div>
  );
};
